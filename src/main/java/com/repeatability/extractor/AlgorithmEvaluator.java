package com.repeatability.extractor;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.repeatability.extractor.gold.GoldAffiliation;
import com.repeatability.extractor.gold.GoldPaper;
import com.repeatability.extractor.util.Pair;

import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;

public class AlgorithmEvaluator implements IAlgorithmEvaluator {
    private static final String FUNDING_OUT_FILE = Config.WORK_DIR + "out/funding_result.csv";
    private static final String WRONG_AFFILIATIONS_OUT_FILE = Config.WORK_DIR + "out/incorrect_affiliations_result.csv";
    private static final String MISSED_AFFILIATIONS_OUT_FILE = Config.WORK_DIR + "out/missed_affiliations_result.csv";
    
    private static final double FUNDING_COSINE_SCORE_THRESHOLD = 0.5;
    private static final double AFFILIATION_COSINE_SCORE_THRESHOLD = 0.35;
    private static final double ARTIFACT_LEV_SCORE_THRESHOLD = 0.5;

    protected FileWriter fundingsWriter;
    
    protected int totalPapers = 0;
    
    protected int falseNegativesEmail = 0;
    protected int truePositivesEmail = 0;
    protected int falsePositivesEmail = 0;

    protected int totalAuthorCount = 0;
    protected int totalEmailCount = 0;

    protected List<Pair<String, String>> wrongEmails = new ArrayList<>();
    protected List<String> missingEmails = new ArrayList<>();

    protected int falseNegativesArtifact = 0;
    protected int truePositivesArtifact = 0;
    protected int falsePositivesArtifact = 0;

    protected int totalArtifactCount = 0;

    protected Set<String> missingArtifacts = new HashSet<>();
    protected Set<String> wrongArtifacts = new HashSet<>();
    
    protected int falseNegativesAffiliation = 0;
    protected int truePositivesAffiliation = 0;
    protected int falsePositivesAffiliation = 0;

    protected int totalAffiliationCount = 0;

    protected Map<String, GoldAffiliation> missingAffiliations = new HashMap<>();
    protected Map<String, Pair<String, GoldAffiliation>> wrongAffiliations = new HashMap<>();

    protected int falseNegativesFunding = 0;
    protected int truePositivesFunding = 0;
    protected int falsePositivesFunding = 0;

    protected int totalFundingCount = 0;

    protected Set<String> missingFunding = new HashSet<>();
    protected Map<String, String> wrongFunding = new HashMap<>();

    public AlgorithmEvaluator() throws IOException {
        fundingsWriter = new FileWriter(FUNDING_OUT_FILE);
    }
    
    protected void evaluateGeneral(GoldPaper goldPaper, PaperInfo paperInfo) {
        System.out.println(paperInfo.getPaperPath());
        System.out.println(paperInfo.getKey());

        totalPapers++;

        List<String> authorsList = goldPaper.getAuthors();
        totalAuthorCount += authorsList.size();
    }
    
    protected void evaluateEmails(GoldPaper goldPaper, PaperInfo paperInfo) {
        System.out.println("\tEmails:");
        for (Map.Entry<String, PaperInfo.MatchedEmail> emailMatch : paperInfo.getAuthorEmails().entrySet()) {
            System.out.println("\t\t" + emailMatch.getKey() + " : " + emailMatch.getValue());
        }
        
        for (String author : goldPaper.getAuthors()) {
            String goldEmail = goldPaper.getEmail(author);
            if (goldEmail == null)
                continue;
            totalEmailCount++;
        
            PaperInfo.MatchedEmail matchedEmail = paperInfo.getAuthorEmails().get(author);
            if (!goldPaper.getEmail(author).isEmpty()) {
                if (matchedEmail == null || matchedEmail.getEmail().isEmpty()) {
                    falseNegativesEmail++;
                    missingEmails.add(goldPaper.getEmail(author));
                } else if (matchedEmail.getEmail().compareToIgnoreCase(goldPaper.getEmail(author)) != 0) {
                    falsePositivesEmail++;
                    wrongEmails.add(new Pair<>(goldPaper.getEmail(author), matchedEmail.getEmail()));
                } else {
                    truePositivesEmail++;
                }
            } else {
                if(matchedEmail == null || matchedEmail.getEmail().isEmpty()) {
                     truePositivesEmail++;
                } else {
                    falsePositivesEmail++;
                }
            }
        }
    }

    protected void evaluateAffiliations(GoldPaper goldPaper, PaperInfo paperInfo) {
        System.out.println("\tAffiliations:");
        for (Map.Entry<String, PaperInfo.MatchedAffiliation> affiliationMatch : paperInfo.getAuthorAffiliations().entrySet()) {
            System.out.println("\t\t" + affiliationMatch.getKey() + " : " + affiliationMatch.getValue());
        }

        List<String> authorsList = goldPaper.getAuthors();
        for (String author : authorsList) {
            PaperInfo.MatchedAffiliation matchedAffiliation = paperInfo.getAuthorAffiliations().get(author);
            GoldAffiliation goldAff = goldPaper.getAffiliation(author);

            if(!goldAff.getInstitution().isEmpty()) {
                totalAffiliationCount++;
            }

            if(goldAff != null && !goldAff.getInstitution().isEmpty()) {
                if (matchedAffiliation == null || matchedAffiliation.getAffiliation().isEmpty()) {
                    falseNegativesAffiliation++;
                    missingAffiliations.put(author, goldAff);
                } else {
                    Cosine cosine = new Cosine();
                    double score = cosine.similarity(goldAff.getInstitution().toLowerCase(),
                            matchedAffiliation.getAffiliation().toLowerCase());

                    if (!matchedAffiliation.getAffiliation().toLowerCase().contains(goldAff.getInstitution().toLowerCase()) &&
                            !goldAff.getInstitution().toLowerCase().contains(matchedAffiliation.getAffiliation().toLowerCase()) &&
                            score < AFFILIATION_COSINE_SCORE_THRESHOLD) {
                    falsePositivesAffiliation++;
                    wrongAffiliations.put(author, new Pair<>(matchedAffiliation.getAffiliation(), goldAff));
                } else {
                    truePositivesAffiliation++;
                }
                }
            } else {
                if(matchedAffiliation == null || matchedAffiliation.getAffiliation().isEmpty()) {
                    truePositivesAffiliation++;
                } else {
                    falsePositivesAffiliation++;
                }
            }
        }
    }
    
    protected void evaluateArtifacts(GoldPaper goldPaper, PaperInfo paperInfo) {
        System.out.println("\tArtifacts:");
        if (paperInfo.getArtifacts() == null)
            return;
        for (PaperInfo.MatchedArtifact artifact : paperInfo.getArtifacts()) {
            System.out.println("\t" + artifact);
        }

        List<String> unseenArtifacts = new ArrayList<>();
        unseenArtifacts.addAll(goldPaper.getArtifacts());

        for (PaperInfo.MatchedArtifact artifact : paperInfo.getArtifacts()) {
            NormalizedLevenshtein nl = new NormalizedLevenshtein();
            double score = 0.;
            String gArtifact="";

            for (String goldArtifact : goldPaper.getArtifacts()) {
                double dist = nl.similarity(artifact.getArtifactLink(), goldArtifact);
                if(dist > score) {
                    gArtifact = goldArtifact;
                    score = dist;
                }
            }

            if(unseenArtifacts.contains(gArtifact)) {
                if (score > ARTIFACT_LEV_SCORE_THRESHOLD) {
                    unseenArtifacts.remove(gArtifact);
                truePositivesArtifact++;
            } else {
                wrongArtifacts.add(artifact.getArtifactLink());
                falsePositivesArtifact++;
            }
            } else {
                wrongArtifacts.add(artifact.getArtifactLink());
                falsePositivesArtifact++;
        }
        }

        missingArtifacts.addAll(unseenArtifacts);
        falseNegativesArtifact += unseenArtifacts.size();
        totalArtifactCount += goldPaper.getArtifacts().size();
    }

    protected void evaluateFunding(GoldPaper goldPaper, PaperInfo paperInfo) throws IOException {
        System.out.println("\tFundings:");
        
        StringBuilder fundings = new StringBuilder();
        StringBuilder fundingInfos = new StringBuilder();
        StringBuilder grants = new StringBuilder();
        fundings.append(paperInfo.getKey() + ",");
        fundingInfos.append("\"");
        grants.append("\"");

        if(goldPaper.getFunding() != null && goldPaper.getFunding().getFunding() != null) {
            totalFundingCount++;
            double score = 0;
            PaperInfo.MatchedFunding matchedFunding = null;
            if (paperInfo.getFundings() != null) {
                for (PaperInfo.MatchedFunding funding : paperInfo.getFundings()) {
                    fundingInfos.append(funding.getFundingInfo());
                    for (String grant : funding.getGrantIds()) {
                        grants.append(grant + " ");
                    }
    
                    System.out.println("\t" + funding);
    
                    Cosine cosine = new Cosine();
                    double s = cosine.similarity(funding.getFundingInfo(), goldPaper.getFunding().getFunding());
                    if(s>score) {
                        score = s;
                        matchedFunding = funding;
                    }
                }
    
                if(matchedFunding != null) {
                    if(score > FUNDING_COSINE_SCORE_THRESHOLD) {
                        truePositivesFunding++;
                    } else {
                        falsePositivesFunding++;
                        wrongFunding.put(matchedFunding.getFundingInfo(), goldPaper.getFunding().getFunding());
                    }
                } else {
                    missingFunding.add(goldPaper.getFunding().getFunding());
                    falseNegativesFunding++;
                }
            }
        }

        fundingInfos.append("\",");
        grants.append("\"\n");

        fundings.append(fundingInfos.toString());
        fundings.append(grants.toString());
        fundingsWriter.flush();
    }
    
    @Override
    public void evaluate(String key, GoldPaper goldPaper, PaperInfo paperInfo) throws IOException {
        evaluateGeneral(goldPaper, paperInfo);
        evaluateEmails(goldPaper, paperInfo);
        evaluateAffiliations(goldPaper, paperInfo);
        evaluateArtifacts(goldPaper, paperInfo);
        evaluateFunding(goldPaper, paperInfo);
    }

    protected void summarizeGeneral() {
        System.out.println();
        System.out.println("Total papers:" + totalPapers);
        System.out.println("Missing email count:" + falseNegativesEmail);
        System.out.println("Wrong email count:" + falsePositivesEmail);
        System.out.println("Correct email count:" + truePositivesEmail);
        System.out.println("Total author count:" + totalAuthorCount);
        System.out.println("Total email count:" + totalEmailCount);

        System.out.println();
        System.out.println("Precision:" + (truePositivesEmail/(float) (truePositivesEmail+falsePositivesEmail)));
        System.out.println("Recall:" + (truePositivesEmail/(float) (truePositivesEmail+falseNegativesEmail)));
    }
    
    protected void summarizeEmails() {
        System.out.println();
        System.out.println("Wrong emails:");
        for (Pair<String, String> pair : wrongEmails) {
            System.out.println("\t" + pair.getKey() + " --- " + pair.getValue());
        }

        System.out.println();
        System.out.println("Missing emails:");
        for (String email : missingEmails) {
            System.out.println("\t" + email);
        }
    }

    protected void summarizeAffiliations() throws IOException {
        System.out.println();
        System.out.println();
        System.out.println("Missing affiliation count:" + falseNegativesAffiliation);
        System.out.println("Wrong affiliation count:" + falsePositivesAffiliation);
        System.out.println("Correct affiliation count:" + truePositivesAffiliation);
        System.out.println("Total author count:" + totalAuthorCount);
        System.out.println("Total affiliation count:" + totalAffiliationCount);

        System.out.println();
        System.out.println("Precision:" + (truePositivesAffiliation/(float) (truePositivesAffiliation+falsePositivesAffiliation)));
        System.out.println("Recall:" + (truePositivesAffiliation/(float) (truePositivesAffiliation+falseNegativesAffiliation)));

        System.out.println();
        System.out.println("Wrong affiliation:");
        try (FileWriter wrongAffWriter = new FileWriter(WRONG_AFFILIATIONS_OUT_FILE)) {
            for (Map.Entry<String, Pair<String, GoldAffiliation>> entry : wrongAffiliations.entrySet()) {
                System.out.println("\t" + entry.getKey() + " --- " + entry.getValue().getKey() + " XXX " + entry.getValue().getValue());
                wrongAffWriter.write("\"" + entry.getKey() + "\"" + "," + "\"" + entry.getValue().getKey() + "\"" + "," + "\"" + entry.getValue().getValue() + "\"" + "\n");
            }
        }

        System.out.println();
        System.out.println("Missing affiliation:");
        try (FileWriter missedAffWriter = new FileWriter(MISSED_AFFILIATIONS_OUT_FILE)) {
            for (Map.Entry<String, GoldAffiliation> entry : missingAffiliations.entrySet()) {
                System.out.println("\t" + entry.getKey() + " --- " + entry.getValue());
                missedAffWriter.write("\"" + entry.getKey() + "\"" + "," + "\"" + entry.getValue() + "\"" + "\n");
            }
        }
    }

    protected void summarizeArtifacts() {
        System.out.println();
        System.out.println();
        System.out.println("Missing artifact count:" + falseNegativesArtifact);
        System.out.println("Wrong artifact count:" + falsePositivesArtifact);
        System.out.println("Correct artifact count:" + truePositivesArtifact);
        System.out.println("Total artifact count:" + totalArtifactCount);

        System.out.println();
        System.out.println("Precision:" + (truePositivesArtifact/(float) (truePositivesArtifact+falsePositivesArtifact)));
        System.out.println("Recall:" + (truePositivesArtifact/(float) (truePositivesArtifact+falseNegativesArtifact)));

        System.out.println();
        System.out.println("Wrong artifact:");
        for (String artifact : wrongArtifacts) {
            System.out.println("\t" + artifact);
        }

        System.out.println();
        System.out.println("Missing artifact:");
        for (String artifact : missingArtifacts) {
            System.out.println("\t" + artifact);
        }
    }
    
    protected void summarizeFunding() {
        System.out.println();
        System.out.println();
        System.out.println("Missing funding count:" + falseNegativesFunding);
        System.out.println("Wrong funding count:" + falsePositivesFunding);
        System.out.println("Correct funding count:" + truePositivesFunding);
        System.out.println("Total funding count:" + totalFundingCount);

        System.out.println();
        System.out.println("Precision:" + (truePositivesFunding/(float) (truePositivesFunding+falsePositivesFunding)));
        System.out.println("Recall:" + (truePositivesFunding/(float) (truePositivesFunding+falseNegativesFunding)));

        System.out.println();
        System.out.println("Wrong funding:");
        for (Map.Entry<String, String> entry : wrongFunding.entrySet()) {
            System.out.println("\t" + entry.getKey() + " ---- " + entry.getValue());
        }

        System.out.println();
        System.out.println("Missing funding:");
        for (String funding : missingFunding) {
            System.out.println("\t" + funding);
        }
    }
    
    @Override
    public void summarize() throws IOException {
        summarizeGeneral();
        summarizeEmails();
        summarizeAffiliations();
        summarizeArtifacts();
        summarizeFunding();
    }

    @Override
    public void close() throws Exception {
        if (fundingsWriter != null)
            fundingsWriter.close();
    }
}
