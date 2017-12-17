package com.repeatability.extractor.scorer;

import java.util.List;

/**
 * Created by savan on 6/20/17.
 */
public interface Scorer<M> {
    List<M> scoreMatches(List<String> args);
}
