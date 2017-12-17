package com.repeatability.extractor.extractors;

import com.repeatability.extractor.Config;
import com.repeatability.extractor.PaperInfo;
import com.repeatability.extractor.matchmakers.similaritymatcher.SplitName;
import com.repeatability.extractor.pdfparsers.pdf2xml.Text;
import com.repeatability.extractor.util.Point2D;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.NodePattern;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;

import java.io.*;
import java.util.*;

/**
 * Created by savan on 7/9/17.
 */
public class AffiliationExtractor implements Extractor<AffiliationExtractor.AffiliationText> {

    public static final double AFFILIATION_RADIUS = 200.;
    private static final String AFFILIATION_REGEX = "[{ner:ORGANIZATION}]+";
    private static final String INSTITUTION_KEYWORDS_FILE = Config.RES_DIR + "institution_keywords.txt";
    private static final String DEPARTMENT_KEYWORDS_FILE = Config.RES_DIR + "department_keywords.txt";

    private static StanfordCoreNLP pipeline;
    private static Set<String> institutionDeptKeywords;

    static {
        pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,ner,regexner",
                        "ssplit.isOneSentence", "true",
                        "regexner.mapping", Config.RES_DIR + "institution-regexner.txt"));
        institutionDeptKeywords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(INSTITUTION_KEYWORDS_FILE))) {
            String institutionKeyword = reader.readLine();
            while (institutionKeyword != null) {
                if(!institutionKeyword.isEmpty())
                    institutionDeptKeywords.add(institutionKeyword.toLowerCase());
                institutionKeyword = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(DEPARTMENT_KEYWORDS_FILE))) {
            String deptKeyword = reader.readLine();
            while (deptKeyword != null) {
                if(!deptKeyword.isEmpty())
                    institutionDeptKeywords.add(deptKeyword.toLowerCase());
                deptKeyword = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<AffiliationExtractor.AffiliationText> extract(PaperInfo paperInfo) {
        Map<String, AuthorText> authorTextMap = getAuthorTexts(paperInfo);
        List<AffiliationExtractor.AffiliationText> probableAffiliations = new ArrayList<>();

        List<Text> texts = paperInfo.getPdf2Xml().getPages().get(0).getTexts();

        for (int i=0; i<texts.size(); i++) {
            Text text = texts.get(i);
            Text prefixText = null;
            if(i!=0) {
                String prefix = texts.get(i-1).getValue();
                if(prefix.length() == 1) {
                    prefixText = texts.get(i-1);
                }
            }

            if(!authorTextMap.containsKey(text)) {
                boolean inVicinity = false;
                for (Map.Entry<String, AuthorText> entry : authorTextMap.entrySet()) {
                    if(inVicinity(entry.getValue().getText(), text)) {
                        inVicinity = true;
                        break;
                    }
                }

                if(inVicinity) {
                    String t = text.getValue().trim();
                    if(!t.isEmpty()) {
                        boolean probableAff = false;
                        if(containsInstitutionKeyword(t.toLowerCase())) {
                            probableAff = true;
                        } else {
                        Annotation document = new Annotation(t);
                        pipeline.annotate(document);

                        List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
                        Env env = TokenSequencePattern.getNewEnv();
                        env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE);
                        TokenSequencePattern pattern = TokenSequencePattern.compile(env, AFFILIATION_REGEX);
                        TokenSequenceMatcher matcher = pattern.getMatcher(tokens);

                        if (matcher.find()) {
                                probableAff = true;
                            }
                        }
                        if(probableAff)
                            probableAffiliations.add(new AffiliationText(text, prefixText));
                        }
                    }
                }
            }

        return probableAffiliations;
        }

    private boolean containsInstitutionKeyword(String t) {
        for (String s : institutionDeptKeywords) {
            if(t.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean inVicinity(Text t1, Text t2) {
        Point2D g1 = t1.getCentroid();
        Point2D g2 = t2.getCentroid();
        double centroidDist = Math.sqrt((g2.getX()-g1.getX())*(g2.getX()-g1.getX()) + (g2.getY()-g1.getY())*(g2.getY()-g1.getY()));

        Point2D tl1 = new Point2D(t1.getLeft(), t1.getTop());
        Point2D tl2 = new Point2D(t2.getLeft(), t2.getTop());
        double tlDist = Math.sqrt((tl2.getX()-tl1.getX())*(tl2.getX()-tl1.getX()) + (tl2.getY()-tl1.getY())*(tl2.getY()-tl1.getY()));

        return Math.min(centroidDist, tlDist) <= AFFILIATION_RADIUS;
    }

    public static Map<String, AuthorText> getAuthorTexts(PaperInfo paperInfo) {
        Map<String, AuthorText> authorTextMap = new HashMap<>();
        List<Text> texts = paperInfo.getPdf2Xml().getPages().get(0).getTexts();
        for (int i=0; i<texts.size(); i++) {
            for (String author : paperInfo.getAuthors()) {
                if(!authorTextMap.containsKey(author) && textContainsName(texts.get(i), new SplitName(author, true))) {
                    Text text = texts.get(i);
                    Text suffixText = null;
                    if(i != texts.size()-1) {
                        String suffix = texts.get(i+1).getValue();
                        if(suffix.length() == 1) {
                            suffixText = texts.get(i+1);
                        }
                    }
                    authorTextMap.put(author, new AuthorText(text, suffixText));
                }
            }
        }
        return authorTextMap;
    }

    private static boolean textContainsName(Text text, SplitName splitName) {
        String textStr = text.getValue().toLowerCase().trim();
        boolean contains = false;
        if(!textStr.isEmpty()) {
            if(textStr.contains(splitName.fullName) ||
                    textStr.contains(splitName.firstName + " " + splitName.lastName) ||
                    textStr.contains(splitName.firstName + " " + splitName.middleName + " " + splitName.lastName) ||
                    textStr.contains(splitName.lastName) ||
                    textStr.contains(splitName.firstName)) {
                contains = true;
            }
        }
        return contains;
    }

    public static class AffiliationText {
        private Text text;
        private Text prefixText;

        public AffiliationText(Text text, Text prefixText) {
            this.text = text;
            this.prefixText = prefixText;
        }

        public Text getText() {
            return text;
        }

        public Text getPrefixText() {
            return prefixText;
        }
    }

    public static class AuthorText {
        private Text text;
        private Text suffixText;

        public AuthorText(Text text, Text suffixText) {
            this.text = text;
            this.suffixText = suffixText;
        }

        public Text getText() {
            return text;
        }

        public Text getSuffixText() {
            return suffixText;
        }
    }

}
