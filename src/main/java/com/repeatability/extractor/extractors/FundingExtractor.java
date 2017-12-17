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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by savan on 6/20/17.
 */
public class FundingExtractor implements Extractor<String> {

    private static final String FUNDING_TOKENS_PREFIX_REGEX =
                "[]*[{lemma:/research|project|work|foundation|support|grant|fund|aid|award|finance|sponsor|thank|partially|part|partly/}]+[]{0,20}[{ner:ORGANIZATION}]+[]*";
    private static final String FUNDING_TOKENS_SUFFIX_REGEX =
            "[{ner:ORGANIZATION}]+[]{0,20}[{lemma:/research|project|work|foundation|support|grant|fund|aid|award|finance|sponsor|thank|partially|part|partly/}]+[]*";

    private static StanfordCoreNLP pipeline;

    static {
        pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,ner,regexner",
                        "regexner.mapping", Config.RES_DIR + "funding-orgs-regexner.txt"));
    }

    @Override
    public List<String> extract(PaperInfo paperInfo) {
        // TODO Add {ner:PERSON} + {support|aid|award, etc}
        List<String> fundings = new ArrayList<>();

        String text = paperInfo.getPdf2Text().getText().replaceAll("-[\\n ]+", "");
        Annotation document = new Annotation(text);
        pipeline.annotate(document);

        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        Env env = TokenSequencePattern.getNewEnv();
        env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE);

        for (CoreMap sentence : sentences) {
            List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
            TokenSequencePattern prefixPattern = TokenSequencePattern.compile(env, FUNDING_TOKENS_PREFIX_REGEX);
            TokenSequenceMatcher prefixMatcher = prefixPattern.getMatcher(tokens);

            if (prefixMatcher.find()) {
                String matchedString = sentence.get(CoreAnnotations.TextAnnotation.class).
                        trim().
                        replaceAll("[\\n]", " ");
                if(!fundings.contains(matchedString)) {
                    fundings.add(matchedString);
                }
            }

            TokenSequencePattern suffixPattern = TokenSequencePattern.compile(env, FUNDING_TOKENS_SUFFIX_REGEX);
            TokenSequenceMatcher suffixMatcher = suffixPattern.getMatcher(tokens);

            if (suffixMatcher.find()) {
                String matchedString = sentence.get(CoreAnnotations.TextAnnotation.class).
                        trim().
                        replaceAll("[\\n]", " ");
                if(!fundings.contains(matchedString)) {
                    fundings.add(matchedString);
                }
            }
        }

        return fundings;
    }
}
