package com.repeatability.extractor.matchmakers.similaritymatcher;

import com.repeatability.extractor.PaperInfo;
import com.repeatability.extractor.matchmakers.Matcher;
import org.apache.lucene.search.spell.LevensteinDistance;

import java.util.*;

/**
 * Created by savan on 6/10/17.
 */
public class LevensteinDist implements Matcher {

//    private PaperInfo paperInfo;

    @Override
    public void setPaperInfo(PaperInfo paperInfo) {
//        this.paperInfo = paperInfo;
    }

    @Override
    public Map<String, List<PaperInfo.Match>> matchByDescPreference(List<String> args, List<String> args2) {
        List<String> emails = args;
        List<String> authors = args2;
        Map<String, List<PaperInfo.Match>> matches = new HashMap<String, List<PaperInfo.Match>>();

        for (String email : emails) {
            matches.put(email, new ArrayList<PaperInfo.Match>());

            for (String author : authors) {
                SplitName splitName = new SplitName(author, true);
                PaperInfo.Match match1 = calcMatchQuotient(author, splitName.firstName + splitName.middleName + splitName.lastName, email);
                PaperInfo.Match match2 = calcMatchQuotient(author, splitName.lastName + splitName.middleName + splitName.firstName, email);
                PaperInfo.Match match3 = calcMatchQuotient(author, splitName.firstName + splitName.lastName, email);
                PaperInfo.Match match4 = calcMatchQuotient(author, splitName.lastName + splitName.firstName, email);
                PaperInfo.Match match5 = calcMatchQuotient(author, splitName.firstName, email);
                PaperInfo.Match match6 = calcMatchQuotient(author, splitName.lastName, email);
                PaperInfo.Match match7 = calcMatchQuotient(author, splitName.firstName + splitName.middleName, email);
                PaperInfo.Match match8 = calcMatchQuotient(author, splitName.middleName + splitName.lastName, email);

                matches.get(email).add(bestMatch(match1, match2, match3, match4, match5, match6, match7, match8));
            }

            sortMatches(matches.get(email));
        }

        return matches;
    }

    private PaperInfo.Match bestMatch(PaperInfo.Match... matches) {
        PaperInfo.Match bestMatch = matches[0];

        for (PaperInfo.Match match : matches) {
            if(match.getConfidenceScore() > bestMatch.getConfidenceScore()) {
                bestMatch = match;
            }
        }

        return bestMatch;
    }

    private void sortMatches(List<PaperInfo.Match> matches) {
        Collections.sort(matches, new Comparator<PaperInfo.Match>() {
            @Override
            public int compare(PaperInfo.Match o1, PaperInfo.Match o2) {
                // Sort descending
                if(o2.getConfidenceScore() < o1.getConfidenceScore()) {
                    return -1;
                } else if(o2.getConfidenceScore() > o1.getConfidenceScore()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    private PaperInfo.Match calcMatchQuotient(String author, String formattedAuthor, String email) {
        LevensteinDistance levDist = new LevensteinDistance();
        float score = levDist.getDistance(email.substring(0, email.indexOf('@')), formattedAuthor);

        int confidenceLevel;
        if(score >= 0.75f) {
            confidenceLevel = PaperInfo.Match.CONFIDENCE_HIGH;
        } else if(score >= 0.6f) {
            confidenceLevel = PaperInfo.Match.CONFIDENCE_MODERATE;
        } else if(score >= 0.4f) {
            confidenceLevel = PaperInfo.Match.CONFIDENCE_LOW;
        } else {
            confidenceLevel = -1;
        }

        return new PaperInfo.MatchedAuthor(author, confidenceLevel, score);
    }

}
