package com.repeatability.extractor;

import java.io.IOException;

import com.repeatability.extractor.gold.GoldPaper;

public interface IAlgorithmEvaluator extends AutoCloseable {
    public void evaluate(String key, GoldPaper goldPaper, PaperInfo paperInfo) throws IOException;
    public void summarize() throws IOException;
    @Override
    public void close() throws Exception;
}
