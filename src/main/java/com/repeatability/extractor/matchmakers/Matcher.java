package com.repeatability.extractor.matchmakers;

import com.repeatability.extractor.PaperInfo;

import java.util.List;
import java.util.Map;

/**
 * Created by savan on 6/10/17.
 */
public interface Matcher {
    void setPaperInfo(PaperInfo paperInfo);
    Map<String, List<PaperInfo.Match>> matchByDescPreference(List<String> args, List<String> args2);
}
