package com.repeatability.extractor.matchmakers;

import com.repeatability.extractor.PaperInfo;

import java.util.List;
import java.util.Map;

/**
 * Created by savan on 6/9/17.
 */
public interface MatchMaker<E, M> {
    Map<String, M> match(List<E> args, PaperInfo paperInfo);
}
