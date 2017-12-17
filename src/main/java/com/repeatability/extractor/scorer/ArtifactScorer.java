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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by savan on 7/11/17.
 */
public class ArtifactScorer implements Scorer<PaperInfo.MatchedArtifact> {

    private static final String ARTIFACT_LINK_REGEX = "[{ner:LINK}]+";

    private static StanfordCoreNLP pipeline;

    static {
        pipeline = new StanfordCoreNLP(
                PropertiesUtils.asProperties(
                        "annotators", "tokenize,ssplit,pos,lemma,ner,regexner",
                        "ssplit.isOneSentence", "true",
                        "regexner.mapping", Config.RES_DIR + "artifact-regexner.txt"));
    }

    @Override
    public List<PaperInfo.MatchedArtifact> scoreMatches(List<String> args) {
        List<PaperInfo.MatchedArtifact> matchedArtifacts = new ArrayList<>();

        for (String arg : args) {
            Annotation document = new Annotation(arg);
            pipeline.annotate(document);

            List<CoreLabel> tokens = document.get(CoreAnnotations.TokensAnnotation.class);
            Env env = TokenSequencePattern.getNewEnv();
            env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE);
            TokenSequencePattern pattern = TokenSequencePattern.compile(env, ARTIFACT_LINK_REGEX);
            TokenSequenceMatcher matcher = pattern.getMatcher(tokens);

            float confidenceScore = 1.f;
            int confidenceLevel = PaperInfo.Match.CONFIDENCE_HIGH;
            while (matcher.find()) {
                String link = matcher.group().toLowerCase().trim();
                matchedArtifacts.add(new PaperInfo.MatchedArtifact(arg, link, confidenceLevel, confidenceScore));
            }
        }

        return matchedArtifacts;
    }
}
