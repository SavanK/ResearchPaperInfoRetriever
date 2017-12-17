package com.repeatability.extractor.example;

import com.repeatability.extractor.Config;
import com.repeatability.extractor.PaperInfo;
import com.repeatability.extractor.RepeatabilityExtractor;
import com.repeatability.extractor.util.Pair;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by savan on 5/24/17.
 */
public class Example {

    private static final String PAPER_PATH = "paperPath";
    private static final String TITLE = "title";
    private static final String AUTHORS = "authors";
    private static final String EMAILS = "emails";

//    @SuppressWarnings("Since15")
    public static void main(String[] args) throws Exception {
        try (RepeatabilityExtractor repeatabilityExtractor = new RepeatabilityExtractor()) {
            Reader in = new FileReader(Config.WORK_DIR + "gold/gold_result.csv");
            

            int falseNegatives = 0;
            int truePositives = 0;
            int falsePositives = 0;

            int totalAuthorCount = 0;
            int totalEmailCount = 0;

            List<Pair<String, String>> wrongEmails = new ArrayList<Pair<String, String>>();
            List<String> missingEmails = new ArrayList<String>();

            try (CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader())) {
	            for (CSVRecord record : parser) {
	                String paperPath = record.get(PAPER_PATH);
	                String title = record.get(TITLE);
	                String authors = record.get(AUTHORS);
	                String goldEmails = record.get(EMAILS);
	
	                List<String> authorsList = new ArrayList<String>();
	                String[] authorsArray = authors.split(",");
	                for (String author : authorsArray) {
	                    authorsList.add(author.trim());
	                }
	                totalAuthorCount += authorsList.size();
	
	                List<String> goldEmailsList = new ArrayList<String>();
	                String[] goldEmailsArray = goldEmails.split(",");
	                for (String goldEmail : goldEmailsArray) {
	                    String email = goldEmail.trim();
	                    goldEmailsList.add(email);
	                    if(!email.isEmpty()) {
	                        totalEmailCount++;
	                    }
	                }
	
	                PaperInfo paperInfo = repeatabilityExtractor.extractInfo(paperPath, title, authorsList);
	
	                System.out.println(paperInfo.getPaperPath());
	                System.out.println(paperInfo.getKey());
	
	                for (Map.Entry<String, PaperInfo.MatchedEmail> emailMatch : paperInfo.getAuthorEmails().entrySet()) {
	                    System.out.println("\t" + emailMatch.getKey() + " : " + emailMatch.getValue());
	                }
	
	                int i=0;
	                for (String author : authorsList) {
	                    PaperInfo.MatchedEmail matchedEmail = paperInfo.getAuthorEmails().get(author);
	                    if(!goldEmailsList.get(i).isEmpty()) {
	                        if (matchedEmail == null || matchedEmail.getEmail().isEmpty()) {
	                            falseNegatives++;
	                            missingEmails.add(goldEmailsList.get(i));
	                        } else if (matchedEmail.getEmail().compareToIgnoreCase(goldEmailsList.get(i)) != 0) {
	                            falsePositives++;
	                            wrongEmails.add(new Pair<>(goldEmailsList.get(i), matchedEmail.getEmail()));
	                        } else {
	                            truePositives++;
	                        }
	                    } else {
	                        if(matchedEmail == null || matchedEmail.getEmail().isEmpty()) {
	                            truePositives++;
	                        } else {
	                            falsePositives++;
	                        }
	                    }
	                    i++;
	                }
	            }
            }
            
            System.out.println();
            System.out.println("Missing email count:" + falseNegatives);
            System.out.println("Wrong email count:" + falsePositives);
            System.out.println("Correct email count:" + truePositives);
            System.out.println("Total author count:" + totalAuthorCount);
            System.out.println("Total email count:" + totalEmailCount);

            System.out.println();
            System.out.println("Precision:" + (truePositives/(float) (truePositives+falsePositives)));
            System.out.println("Recall:" + (truePositives/(float) (truePositives+falseNegatives)));

            System.out.println();
            System.out.println("Wrong emails:");
            for (Pair<String, String> pair : wrongEmails) {
                System.out.println("\t" + pair.getKey() + " --- " + pair.getValue());
            }

            System.out.println();
            System.out.println("Missing emails:");
            for (String missingEmail : missingEmails) {
                System.out.println("\t" + missingEmail);
            }
        } catch (JAXBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}