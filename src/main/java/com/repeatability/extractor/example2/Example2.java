package com.repeatability.extractor.example2;

import com.repeatability.extractor.AlgorithmEvaluator;
import com.repeatability.extractor.Config;
import com.repeatability.extractor.PaperInfo;
import com.repeatability.extractor.RepeatabilityExtractor;
import com.repeatability.extractor.gold.GoldAffiliation;
import com.repeatability.extractor.gold.GoldFunding;
import com.repeatability.extractor.gold.GoldPaper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * Created by savan on 6/18/17.
 */

public class Example2 {
    private static final String KEY = "key";
    private static final String AUTHOR_NAME = "name";
    private static final String AUTHOR_EMAIL = "thenEmail";
    private static final String EMAIL_WHAT = "thenEmailWhat";
    private static final String ARTIFACTS = "artifacts";
    private static final String INSTITUTION = "thenInstitution";
    private static final String DEPARTMENT = "thenDepartment";
    private static final String FUNDING = "funding";
    private static final String NSF_FUNDING = "nsfFunding";
    private static final String FILTER = "conf/naacl/Moosavi016";

    private static final boolean DO_FILTER = false;

    private static final String NULL_EMAIL = "NullEmail";

    public static boolean doFilter() {
        return DO_FILTER;
    }
    
    protected void readEmails(Map<String, GoldPaper> goldPaperMap) throws FileNotFoundException, IOException {
        try (Reader in = new FileReader(Config.WORK_DIR + "gold/GoldStandard_emails.csv")) {
            try (CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader())) {
    
                for (CSVRecord record : parser) {
                    String key = record.get(KEY);
                    String authorName = record.get(AUTHOR_NAME);
                    String emailWhat = record.get(EMAIL_WHAT);
                    String authorEmail = "";
        
                    if(emailWhat.compareToIgnoreCase(NULL_EMAIL) != 0) {
                        authorEmail = record.get(AUTHOR_EMAIL);
                    }
        
                    if(!doFilter() || key.compareToIgnoreCase(FILTER) == 0) {
                        GoldPaper goldPaper = goldPaperMap.get(key);
                        if (goldPaper == null) {
                            goldPaper = new GoldPaper(key);
                            goldPaperMap.put(key, goldPaper);
                        }
        
                        goldPaper.addAuthorEmail(authorName, authorEmail);
                    }
                }
            }
        }
    }

    protected void readArtifacts(Map<String, GoldPaper> goldPaperMap) throws FileNotFoundException, IOException {
        try (Reader in = new FileReader(Config.WORK_DIR + "gold/GoldStandard_artifacts.csv")) {
            try (CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader())) {
                for (CSVRecord record : parser) {
                    String key = record.get(KEY);
                    String artifactStr = record.get(ARTIFACTS);
        
                    if(!doFilter() || key.compareToIgnoreCase(FILTER) == 0) {
                        GoldPaper goldPaper = goldPaperMap.get(key);
                        String[] artifacts = artifactStr.split("[\\ ]+");
                        for (String artifact : artifacts) {
                            if(!artifact.isEmpty())
                                goldPaper.addArtifact(artifact);
                        }
                    }
                }
            }
        }
    }

    protected void readAffiliations(Map<String, GoldPaper> goldPaperMap) throws FileNotFoundException, IOException {
        try (Reader in = new FileReader(Config.WORK_DIR + "gold/GoldStandard_affiliations.csv")) {
            try (CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader())) {
                for (CSVRecord record : parser) {
                    String key = record.get(KEY);
                    String institution = record.get(INSTITUTION);
                    String department = record.get(DEPARTMENT);
                    String author = record.get(AUTHOR_NAME);
        
                    if(!doFilter() || key.compareToIgnoreCase(FILTER) == 0) {
                        GoldPaper goldPaper = goldPaperMap.get(key);
                        goldPaper.addAuthorAffiliation(author, new GoldAffiliation(institution, department));
                    }
                }
            }
        }
    }

    protected void readFunding(Map<String, GoldPaper> goldPaperMap) throws FileNotFoundException, IOException {
        try (Reader in = new FileReader(Config.WORK_DIR + "gold/GoldStandard_funding.csv")) {
            try (CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader())) {
                for (CSVRecord record : parser) {
                    String key = record.get(KEY);
                    String fundingStr = record.get(FUNDING);
                    String nsfFundingStr = record.get(NSF_FUNDING);
        
                    if(!doFilter() || key.compareToIgnoreCase(FILTER) == 0) {
                        if(!fundingStr.isEmpty()) {
                            GoldPaper goldPaper = goldPaperMap.get(key);
                            GoldFunding funding = new GoldFunding(fundingStr, nsfFundingStr);
                            goldPaper.setFunding(funding);
                        }
                    }
                }
            }
        }
    }
    
    protected Map<String, GoldPaper> getGoldPaperMap() throws FileNotFoundException, IOException {
        Map<String, GoldPaper> goldPaperMap = new HashMap<>();
    
        readEmails(goldPaperMap);
        readArtifacts(goldPaperMap);
        readAffiliations(goldPaperMap);
        readFunding(goldPaperMap);
        return goldPaperMap;
    }
    
    public void run() {
        try (RepeatabilityExtractor repeatabilityExtractor = new RepeatabilityExtractor()) {
            try (AlgorithmEvaluator algorithmEvaluator = new AlgorithmEvaluator()) {
                Map<String, GoldPaper> goldPaperMap = getGoldPaperMap();
                
                for (Map.Entry<String, GoldPaper> entry : goldPaperMap.entrySet()) {
                    String key = entry.getKey();
                    GoldPaper goldPaper = entry.getValue();
                    List<String> authorsList = goldPaper.getAuthors();
                    PaperInfo paperInfo = repeatabilityExtractor.extractInfo(
                            goldPaper.getPaperPath(), goldPaper.getKey(), authorsList);
    
                    algorithmEvaluator.evaluate(key, goldPaper, paperInfo);
                }
                algorithmEvaluator.summarize();
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        new Example2().run();
    }
}
