package com.repeatability.extractor.matchmakers;

import com.repeatability.extractor.PaperInfo;
import com.repeatability.extractor.matchmakers.similaritymatcher.EmailPermutermIndex;
import com.repeatability.extractor.matchmakers.similaritymatcher.LevensteinDist;
import com.repeatability.extractor.matchmakers.similaritymatcher.NGramDist;
import com.repeatability.extractor.matchmakers.similaritymatcher.SplitName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by savan on 6/7/17.
 */
public class EmailMatchMaker implements MatchMaker<String, PaperInfo.MatchedEmail> {

    private static final String DUMMY = "XXXXXXXXXX";

    private static final String GENERIC_FIRST_LAST = "first.last";
    private static final String GENERIC_LAST_FIRST = "last.first";
    private static final String GENERIC_LAST = "last";
    private static final String GENERIC_FIRST = "first";

    @Override
    public Map<String, PaperInfo.MatchedEmail> match(List<String> args, PaperInfo paperInfo) {
        String genericEmail = findGenericEmail(args);
        List<String> emails = filterEmails(args);

        List<String> normalizedAuthors = getNormalizedAuthors(paperInfo.getAuthors(), emails);
        List<String> normalizedEmails = getNormalizedEmails(paperInfo.getAuthors(), emails);

        Map<String, List<PaperInfo.Match>> permutermMatches = new EmailPermutermIndex().matchByDescPreference(normalizedEmails, normalizedAuthors);
        Map<String, List<PaperInfo.Match>> nGramMatches = new NGramDist().matchByDescPreference(normalizedEmails, normalizedAuthors);
        Map<String, List<PaperInfo.Match>> levMatches = new LevensteinDist().matchByDescPreference(normalizedEmails, normalizedAuthors);

        String[][] authorPrefs = new String[normalizedAuthors.size()][normalizedEmails.size()];
        int i=0;
        for (String author : normalizedAuthors) {
            for (int j = 0; j < normalizedEmails.size(); j++) {
                if(permutermMatches.containsKey(author) && j < permutermMatches.get(author).size()) {
                    authorPrefs[i][j] = ((PaperInfo.MatchedEmail)permutermMatches.get(author).get(j)).getEmail();
                } else {
                    authorPrefs[i][j] = normalizedEmails.get(j);
                }
            }
            i++;
        }

        Map<String, List<PaperInfo.Match>> nGramAndLevMatches = new HashMap<String, List<PaperInfo.Match>>();
        for (Map.Entry<String, List<PaperInfo.Match>> nGramEntry : nGramMatches.entrySet()) {
            List<PaperInfo.Match> combinedMatches = new ArrayList<PaperInfo.Match>();

            int k=0;
            for (PaperInfo.Match nGramMatch : nGramEntry.getValue()) {
                float combinedConfidenceScore = .5f * (nGramMatch.getConfidenceScore() +
                        levMatches.get(nGramEntry.getKey()).get(k).getConfidenceScore());
                int combinedConfidenceLevel = getConfidenceLevel(combinedConfidenceScore);

                PaperInfo.Match combinedMatch = new PaperInfo.MatchedAuthor(
                        ((PaperInfo.MatchedAuthor)nGramMatch).getAuthor(),
                        combinedConfidenceLevel,
                        combinedConfidenceScore);
                combinedMatches.add(combinedMatch);

                k++;
            }

            nGramAndLevMatches.put(nGramEntry.getKey(), combinedMatches);
        }

        String[][] emailPrefs = new String[normalizedEmails.size()][normalizedAuthors.size()];
        i=0;
        for (String email : normalizedEmails) {
            for (int j = 0; j < normalizedAuthors.size(); j++) {
                if(nGramAndLevMatches.containsKey(email) && j < nGramAndLevMatches.get(email).size()) {
                    emailPrefs[i][j] = ((PaperInfo.MatchedAuthor)nGramAndLevMatches.get(email).get(j)).getAuthor();
                } else {
                    emailPrefs[i][j] = normalizedAuthors.get(j);
                }
            }
            i++;
        }

        String[] authorsArray = new String[normalizedAuthors.size()];
        String[] emailsArray = new String[normalizedEmails.size()];

        i=0;
        for (String author : normalizedAuthors) {
            authorsArray[i] = author;
            i++;
        }

        i=0;
        for (String email : normalizedEmails) {
            emailsArray[i] = email;
            i++;
        }

        StableMarriage stableMarriage = new StableMarriage(
                authorsArray, emailsArray, authorPrefs, emailPrefs);
        stableMarriage.calculateMatches();

        Map<String, String> couples = stableMarriage.getCouples();

        Map<String, PaperInfo.MatchedEmail> overallScoredMatches = new HashMap<String, PaperInfo.MatchedEmail>();

        for (Map.Entry<String, String> couple : couples.entrySet()) {
            String author = couple.getKey();
            String email = couple.getValue();

            if(!author.contains(DUMMY)) {
                if (!email.contains(DUMMY)){
                    PaperInfo.Match permutermMatch = null;
                    for (PaperInfo.Match match : permutermMatches.get(author)) {
                        if (((PaperInfo.MatchedEmail) match).getEmail().compareToIgnoreCase(email) == 0) {
                            permutermMatch = match;
                            break;
                        }
                    }

                    PaperInfo.Match nGramLevMatch = null;
                    for (PaperInfo.Match match : nGramAndLevMatches.get(email)) {
                        if (((PaperInfo.MatchedAuthor) match).getAuthor().compareToIgnoreCase(author) == 0) {
                            nGramLevMatch = match;
                            break;
                        }
                    }

                    float multiplyingFactor = (permutermMatch.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_HIGH ? 1.f :
                            permutermMatch.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_MODERATE ? .75f :
                                    permutermMatch.getConfidenceLevel() == PaperInfo.Match.CONFIDENCE_LOW ? .6f : 0.f);
                    float overallScore = .5f * (permutermMatch.getConfidenceScore() * multiplyingFactor + nGramLevMatch.getConfidenceScore());
                    PaperInfo.MatchedEmail overallMatch = new PaperInfo.MatchedEmail(email, getConfidenceLevel(overallScore), overallScore);
                    overallScoredMatches.put(author, overallMatch);
                } else if(!genericEmail.isEmpty()) {
                    String gEmail = "";
                    SplitName splitName = new SplitName(author, false);

                    String gUsername = genericEmail.substring(0, genericEmail.indexOf('@'));
                    String gDomain = genericEmail.substring(genericEmail.indexOf('@'));

                    if(gUsername.equals(GENERIC_FIRST)) {
                        gEmail = splitName.firstName + gDomain;
                    } else if(gUsername.equals(GENERIC_LAST)) {
                        gEmail = splitName.lastName + gDomain;
                    } else if(gUsername.equals(GENERIC_FIRST_LAST)) {
                        gEmail = splitName.firstName + "." + splitName.lastName + gDomain;
                    } else if(gUsername.equals(GENERIC_LAST_FIRST)) {
                        gEmail = splitName.lastName + "." + splitName.firstName + gDomain;
                    }

                    PaperInfo.MatchedEmail match = new PaperInfo.MatchedEmail(gEmail, PaperInfo.Match.CONFIDENCE_HIGH, 1.0f);
                    overallScoredMatches.put(author, match);
                }
            }
        }

        return overallScoredMatches;
    }

    private List<String> filterEmails(List<String> emails) {
        List<String> filteredEmails = new ArrayList<>();

        for (String email : emails) {
            String username = email.substring(0, email.indexOf('@'));
            if(!username.contains("firstname") && !username.contains("lastname")
                    && !username.equals("first.last") && !username.equals("last.first")
                    && !username.equals("first") && !username.equals("last")) {
                filteredEmails.add(email);
            }
        }

        return filteredEmails;
    }

    private String findGenericEmail(List<String> emails) {
        String genericEmail = "";

        for (String email : emails) {
            String username = email.substring(0, email.indexOf('@'));
            String domain = email.substring(email.indexOf('@'));
            if(username.equals("firstname.lastname")) {
                genericEmail = GENERIC_FIRST_LAST + domain;
            } else if(username.equals("lastname.firstname")) {
                genericEmail = GENERIC_LAST_FIRST + domain;
            } else if(username.equals("last.first")) {
                genericEmail = GENERIC_LAST_FIRST + domain;
            } else if(username.equals("first.last")) {
                genericEmail = GENERIC_FIRST_LAST + domain;
            } else if(username.equals("first")) {
                genericEmail = GENERIC_FIRST + domain;
            } else if(username.equals("last")) {
                genericEmail = GENERIC_LAST + domain;
            }

            if(!genericEmail.isEmpty())
                break;
        }

        return genericEmail;
    }

    private List<String> getNormalizedAuthors(List<String> authors, List<String> emails) {
        List<String> normalizedAuthors = new ArrayList<String>();
        for (int i = 0; i < Math.max(authors.size(), emails.size()); i++) {
            if(i < authors.size()) {
                normalizedAuthors.add(authors.get(i));
            } else {
                normalizedAuthors.add(DUMMY + i);
            }
        }
        return normalizedAuthors;
    }

    private List<String> getNormalizedEmails(List<String> authors, List<String> emails) {
        List<String> normalizedEmails = new ArrayList<String>();
        for (int i = 0; i < Math.max(authors.size(), emails.size()); i++) {
            if(i < emails.size()) {
                normalizedEmails.add(emails.get(i));
            } else {
                normalizedEmails.add(DUMMY + i + "@" + DUMMY);
            }
        }
        return normalizedEmails;
    }

    private int getConfidenceLevel(float confidenceScore) {
        int confidenceLevel;
        if(confidenceScore >= 0.75f) {
            confidenceLevel = PaperInfo.Match.CONFIDENCE_HIGH;
        } else if(confidenceScore >= 0.6f) {
            confidenceLevel = PaperInfo.Match.CONFIDENCE_MODERATE;
        } else if(confidenceScore >= 0.4f) {
            confidenceLevel = PaperInfo.Match.CONFIDENCE_LOW;
        } else {
            confidenceLevel = -1;
        }

        return confidenceLevel;
    }

/*    private class OverallMatch implements PaperInfo.Match {
        private int confidenceLevel;
        private float confidenceScore;

        public int getConfidenceLevel() {
            return confidenceLevel;
        }

        public float getConfidenceScore() {
            return confidenceScore;
        }

        public void setConfidenceLevel(int level) {
            confidenceLevel = level;
        }

        public void setConfidenceScore(float score) {
            confidenceScore = score;
        }
    }*/
}
