package com.repeatability.extractor.scorer;

import com.repeatability.extractor.Config;
import com.repeatability.extractor.PaperInfo;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.NodePattern;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by savan on 6/20/17.
 */
public class FundingScorer implements Scorer<PaperInfo.MatchedFunding> {
    private static final String KNOWN_ORGS_FILE = Config.RES_DIR + "known_funding_orgs.txt";
    private static final String COUNTRY_ADJECTIVALS_FILE = Config.RES_DIR + "country_adjectivals.csv";
    private static final String NSF_PROGRAMS_FILE = Config.RES_DIR + "NsfPrograms.csv";

    private static final String COUNTRY = "Country";
    private static final String ADJECTIVALS = "Adjectivals";
    private static final String PROGRAM = "Program";

    private static final String ORGANIZATION_REGEX =
            "[{ner:ORGANIZATION}]+";
    private static final String SUPPORTING_WORDS =
            "[{lemma:/research|project|work|foundation|support|grant|fund|aid|award|university|finance|sponsor|thank|partially|part|partly/}]";

    private static StanfordCoreNLP pipeline;
    private static Set<String> knownFundingOrgs;

    private static String countriesRE;
    private static String adjectivalsRE;

    private static String nsfProgramsRE = "[ ,.-:](?)[ -]*((?)[ -]*)*[0-9]+([, ]+[0-9]+)*";
    private static String otherProgramsRE =
            "(((((g|G)rant(|s))|((a|A)ward(|s)))[ \\n]*|)(((a|A)greement(|s))[. (]*[0-9A-Z-/]{3,})([,][ ]*[0-9A-Z-/]{3,})*[)]*|((((g|G)rant(|s))|((a|A)ward(|s)))[ \\n]*|)(((n|N)(o|(umber(s|))))[. (]*[0-9A-Z-/]{3,})([,][ ]*[0-9A-Z-/]{3,})*[)]*|(((g|G)rant(|s))|((a|A)ward(|s)))[ -.]*[0-9A-Z-/]{3,}([,][ ]*[0-9A-Z-/]{3,})*[)]*)";

    static {
        pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,ner",
                        "ssplit.isOneSentence", "true"));
        knownFundingOrgs = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(KNOWN_ORGS_FILE))) {
            String org = reader.readLine();
            while (org != null) {
                knownFundingOrgs.add(org.toLowerCase());
                org = reader.readLine();
            }

            StringBuilder countriesBuilder = new StringBuilder();
            StringBuilder adjectivalsBuilder = new StringBuilder();
            countriesBuilder.append("(");
            adjectivalsBuilder.append("(");
            
            try (Reader in = new FileReader(COUNTRY_ADJECTIVALS_FILE)) {
                try (CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader())) {
                    for (CSVRecord record : parser) {
                        String country = record.get(COUNTRY).toLowerCase().trim();
                        String adjective = record.get(ADJECTIVALS).toLowerCase().trim();
        
                        countriesBuilder.append(country);
                        countriesBuilder.append("|");
                        adjectivalsBuilder.append(adjective);
                        adjectivalsBuilder.append("|");
                    }
                }
            }

            countriesBuilder.replace(countriesBuilder.length()-1, countriesBuilder.length(), ")");
            adjectivalsBuilder.replace(adjectivalsBuilder.length()-1,adjectivalsBuilder.length(), ")");

            countriesRE = countriesBuilder.toString();
            adjectivalsRE = adjectivalsBuilder.toString();

            StringBuilder nsfPrograms = new StringBuilder();

            try (Reader in = new FileReader(NSF_PROGRAMS_FILE)) {
                try (CSVParser parser = new CSVParser(in, CSVFormat.EXCEL.withHeader())) {
                    for (CSVRecord record : parser) {
                        String program = record.get(PROGRAM).toLowerCase().trim();

                        nsfPrograms.append(program);
                        nsfPrograms.append("|");
                    }
                }
            }
            nsfPrograms.delete(nsfPrograms.length()-1, nsfPrograms.length());
            nsfProgramsRE = nsfProgramsRE.replaceAll("[?]", nsfPrograms.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<PaperInfo.MatchedFunding> scoreMatches(List<String> args) {
        List<PaperInfo.MatchedFunding> fundings = new ArrayList<>();
        for (String arg : args) {
            Set<String> fundingOrgs = new HashSet<>();
            Set<String> grantIds = new HashSet<>();

            Annotation document = new Annotation(arg);
            pipeline.annotate(document);

            List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
            Env env = TokenSequencePattern.getNewEnv();
            env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE);
            TokenSequencePattern pattern = TokenSequencePattern.compile(env, ORGANIZATION_REGEX);
            TokenSequenceMatcher matcher = pattern.getMatcher(tokens);

            float confidenceScore = 0.f;
            boolean organizationKnown = false;
            while (matcher.find()) {
                String organization = matcher.group();
                fundingOrgs.add(organization);
                if(!organizationKnown)
                    organizationKnown = knownFundingOrgs.contains(organization.toLowerCase());
            }

            if(organizationKnown) {
                confidenceScore = 1.f;
            } else {
                boolean hasCountryAffiliation;
                Pattern p = Pattern.compile(countriesRE);
                Matcher m = p.matcher(arg.toLowerCase());
                if(m.find()) {
                    hasCountryAffiliation = true;
                } else {
                    p = Pattern.compile(adjectivalsRE);
                    m = p.matcher(arg.toLowerCase());
                    hasCountryAffiliation = m.find();
                }

                if(hasCountryAffiliation)
                    confidenceScore += .3f;

                pattern = TokenSequencePattern.compile(env, SUPPORTING_WORDS);
                matcher = pattern.getMatcher(tokens);
                int supportWordsCount = 0;
                while (matcher.find()) {
                    supportWordsCount++;
                }

                confidenceScore += Math.min(supportWordsCount, 4) * 0.1f;
            }

            Pattern nsfPrograms = Pattern.compile(nsfProgramsRE);
            Matcher nsfProgramsMatcher = nsfPrograms.matcher(arg.toLowerCase());
            boolean hasNsfPrograms = false;
            while (nsfProgramsMatcher.find()) {
                String grantId = nsfProgramsMatcher.group();
                grantIds.add(grantId);
                if(!hasNsfPrograms)
                    hasNsfPrograms = true;
            }

            Pattern otherPrograms = Pattern.compile(otherProgramsRE);
            Matcher otherProgramsMatcher = otherPrograms.matcher(arg);
            boolean hasOtherPrograms = false;
            while (otherProgramsMatcher.find()) {
                String grantId = otherProgramsMatcher.group();
                grantIds.add(grantId);
                if(!hasOtherPrograms)
                    hasOtherPrograms = true;
            }

            if(hasNsfPrograms)
                confidenceScore = 1.f;
            else if(hasOtherPrograms)
                confidenceScore = Math.min(1.f, confidenceScore+.2f);

            int confidenceLevel = -1;
            if(confidenceScore >= .7f) {
                confidenceLevel = PaperInfo.Match.CONFIDENCE_HIGH;
            } else if(confidenceScore >= .5f) {
                confidenceLevel = PaperInfo.Match.CONFIDENCE_MODERATE;
            } else if(confidenceScore >= .3f) {
                confidenceLevel = PaperInfo.Match.CONFIDENCE_LOW;
            }

            PaperInfo.MatchedFunding funding = new PaperInfo.MatchedFunding(arg, fundingOrgs, grantIds, confidenceLevel, confidenceScore);
            fundings.add(funding);
        }

        return fundings;
    }

}
