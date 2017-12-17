package com.repeatability.extractor.extractors;

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
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by savan on 7/10/17.
 */
public class ArtifactExtractor implements Extractor<String> {
    private static final String ARTIFACT_TOKEN_PREFIX_REGEX =
            "[]*[{lemma:/public|publicly|available|source|code|git|github|bitbucket/}]+[]{0,10}[{ner:LINK}]+[]*";
    private static final String ARTIFACT_TOKEN_SUFFIX_REGEX =
            "[{ner:LINK}]+[]{0,10}[{lemma:/public|publicly|available|source|code|git|github|bitbucket/}]+[]*";
    private static final String ARTIFACT_LINK_REGEX =
            "(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)";
    private static final String REFERENCE_SECTION_REGEX = "\\b((R)(eference|EFERENCE)[sS]?)$";
    private static final String ARTIFACT_NER_LINK_REGEX = "[{ner:LINK}]+";

    private static final String ARTIFACT_KEYWORDS_FILE = Config.RES_DIR + "artifact_keywords.txt";

    private static StanfordCoreNLP pipeline;
    private static Set<String> artifactKeywords;

    static {
        pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,ner,regexner",
                        "regexner.mapping", Config.RES_DIR + "artifact-regexner.txt"));
        artifactKeywords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(ARTIFACT_KEYWORDS_FILE))){
            String artifactKeyword = reader.readLine();
            while (artifactKeyword != null) {
                if(!artifactKeyword.isEmpty())
                    artifactKeywords.add(artifactKeyword.toLowerCase());
                artifactKeyword = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean containsArtifactKeyword(String t) {
        for (String s : artifactKeywords) {
            if(t.contains(s)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> extract(PaperInfo paperInfo) {
        List<String> artifacts = new ArrayList<>();
        String text = paperInfo.getPdf2Text().getText().replaceAll("-[\\n ]+", "");
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Env env = TokenSequencePattern.getNewEnv();
        env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE);

        for (CoreMap sentence : sentences) {
            String sen = sentence.get(CoreAnnotations.TextAnnotation.class).
                    //replaceAll("(\\n )","").
                    replaceAll("[\\n]", "").trim();
            Pattern referencePattern = Pattern.compile(REFERENCE_SECTION_REGEX);
            Matcher referenceMatcher = referencePattern.matcher(sen);
            if(referenceMatcher.find()) {
                // avoid looking at links after reference section
                break;
            }

            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            TokenSequencePattern prefixPattern = TokenSequencePattern.compile(env, ARTIFACT_TOKEN_PREFIX_REGEX);
            TokenSequenceMatcher prefixMatcher = prefixPattern.getMatcher(tokens);

            if (prefixMatcher.find()) {
                String matchedString = sentence.get(CoreAnnotations.TextAnnotation.class).
                        trim().
                        //replaceAll("(\\n )","").
                        replaceAll("[\\n]", "");
                if(!artifacts.contains(matchedString)) {
                    artifacts.add(matchedString);
                }
            }

            TokenSequencePattern suffixPattern = TokenSequencePattern.compile(env, ARTIFACT_TOKEN_SUFFIX_REGEX);
            TokenSequenceMatcher suffixMatcher = suffixPattern.getMatcher(tokens);

            if (suffixMatcher.find()) {
                String matchedString = sentence.get(CoreAnnotations.TextAnnotation.class).
                        trim().
                        //replaceAll("(\\n )","").
                        replaceAll("[\\n]", "");
                if(!artifacts.contains(matchedString)) {
                    artifacts.add(matchedString);
                }
            }

            TokenSequencePattern linkNerPattern = TokenSequencePattern.compile(env, ARTIFACT_NER_LINK_REGEX);
            TokenSequenceMatcher linkNerMatcher = linkNerPattern.getMatcher(tokens);

            if (linkNerMatcher.find()) {
                String matchedString = sentence.get(CoreAnnotations.TextAnnotation.class).toLowerCase().
                        trim().
                        replaceAll("[\\n]", "");
                if (!artifacts.contains(matchedString) && containsArtifactKeyword(sen.toLowerCase())) {
                    artifacts.add(matchedString);
                }
            }

            Pattern linkPattern = Pattern.compile(ARTIFACT_LINK_REGEX);
            Matcher linkMatcher = linkPattern.matcher(sen);

            if(linkMatcher.find()) {
                if(!artifacts.contains(sen) && containsArtifactKeyword(sen.toLowerCase())) {
                    artifacts.add(sen);
                }
            }
        }

        return artifacts;
    }
}
