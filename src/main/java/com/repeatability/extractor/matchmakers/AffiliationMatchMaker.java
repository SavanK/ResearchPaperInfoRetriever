package com.repeatability.extractor.matchmakers;

import com.repeatability.extractor.Config;
import com.repeatability.extractor.PaperInfo;
import com.repeatability.extractor.extractors.AffiliationExtractor;
import com.repeatability.extractor.pdfparsers.pdf2xml.Text;
import com.repeatability.extractor.util.Point2D;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by savan on 7/9/17.
 */
public class AffiliationMatchMaker implements MatchMaker<AffiliationExtractor.AffiliationText, PaperInfo.MatchedAffiliation> {

    private static final String INSTITUTION_KEYWORDS_FILE = Config.RES_DIR + "institution_keywords.txt";
    private static final String DEPARTMENT_KEYWORDS_FILE = Config.RES_DIR + "department_keywords.txt";

    private static final float INSTITUTION_SCORE_BOOST = .25f;
    private static final float DEPARTMENT_SCORE_REDUCTION = -.15f;

    private static Set<String> institutionKeywords;
    private static Set<String> deptKeywords;

    static {
        institutionKeywords = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(INSTITUTION_KEYWORDS_FILE))) {
            String institutionKeyword = reader.readLine();
            while (institutionKeyword != null) {
                if(!institutionKeyword.isEmpty())
                    institutionKeywords.add(institutionKeyword.toLowerCase());
                institutionKeyword = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        deptKeywords = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(DEPARTMENT_KEYWORDS_FILE))) {
            String deptKeyword = reader.readLine();
            while (deptKeyword != null) {
                if(!deptKeyword.isEmpty())
                    deptKeywords.add(deptKeyword.toLowerCase());
                deptKeyword = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, PaperInfo.MatchedAffiliation> match(List<AffiliationExtractor.AffiliationText> args, PaperInfo paperInfo) {
        Map<String, PaperInfo.MatchedAffiliation> matches = new HashMap<>();
        List<AffiliationExtractor.AffiliationText> affiliationTexts = args;

        Map<String, AffiliationExtractor.AuthorText> authorTextMap = AffiliationExtractor.getAuthorTexts(paperInfo);

        for (String author : paperInfo.getAuthors()) {
            AffiliationExtractor.AuthorText authorText = authorTextMap.get(author);
            String affiliation = "";
            int confidenceLevel = -1;
            float confidenceScore = 0.f;
            boolean superScript = false;

            if(authorText != null) {
                float maxScore = 0.f;

                for (AffiliationExtractor.AffiliationText affiliationText : affiliationTexts) {
                    if(authorText.getSuffixText() != null && affiliationText.getPrefixText() != null &&
                            authorText.getSuffixText().getValue().toLowerCase().equals(
                            affiliationText.getPrefixText().getValue().toLowerCase())) {
                        superScript = true;
                        affiliation = affiliationText.getText().getValue();
                        break;
                    }
                    if (authorText.getText().getTop() <= affiliationText.getText().getTop()) {
                        double distance = distance(authorText.getText(), affiliationText.getText());
                        String aff = affiliationText.getText().getValue();
                        float score = calcConfidenceScore(distance, aff);
                        if (score > maxScore) {
                            maxScore = score;
                            affiliation = aff;
                        }
                    }
                }

                if(!affiliation.isEmpty()) {
                    if(superScript) {
                        confidenceLevel = PaperInfo.Match.CONFIDENCE_HIGH;
                        confidenceScore = 1.f;
                    } else {
                        confidenceScore = maxScore;
                        confidenceLevel = calcConfidenceLevel(confidenceScore);
                    }
                }
            }

            matches.put(author, new PaperInfo.MatchedAffiliation(affiliation, confidenceLevel, confidenceScore));
        }

        return matches;
    }

    private double distance(Text t1, Text t2) {
        Point2D g1 = t1.getCentroid();
        Point2D g2 = t2.getCentroid();

        return Math.sqrt((g2.getX()-g1.getX())*(g2.getX()-g1.getX()) + (g2.getY()-g1.getY())*(g2.getY()-g1.getY()));
    }

    private float calcConfidenceScore(double distance, String affiliation) {
        float score=0.f;
        if(distance <= AffiliationExtractor.AFFILIATION_RADIUS) {
            score = (float) ((AffiliationExtractor.AFFILIATION_RADIUS - distance) / AffiliationExtractor.AFFILIATION_RADIUS);
        }

        if(hasInstitutionKeyword(affiliation.toLowerCase())) {
            score += INSTITUTION_SCORE_BOOST;
        }

        if(hasDeptKeyword(affiliation.toLowerCase())) {
            score += DEPARTMENT_SCORE_REDUCTION;
        }

        if(score < 0)
            score = 0.f;
        else if(score > 1)
            score = 1.f;

        return score;
    }

    private int calcConfidenceLevel(float confidenceScore) {
        if(confidenceScore >= .65f) {
            return PaperInfo.Match.CONFIDENCE_HIGH;
        } else if(confidenceScore >= .4f) {
            return PaperInfo.Match.CONFIDENCE_MODERATE;
        } else if(confidenceScore >= .2f) {
            return PaperInfo.Match.CONFIDENCE_LOW;
        } else {
            return -1;
        }
    }

    private boolean hasInstitutionKeyword(String aff) {
        for (String instKeyword : institutionKeywords) {
            if(aff.contains(instKeyword))
                return true;
        }
        return false;
    }

    private boolean hasDeptKeyword(String aff) {
        for (String deptKeyword : deptKeywords) {
            if(aff.contains(deptKeyword))
                return true;
        }
        return false;
    }
}
