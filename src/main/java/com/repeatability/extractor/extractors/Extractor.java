package com.repeatability.extractor.extractors;

import com.repeatability.extractor.PaperInfo;

import java.util.List;

/**
 * Created by savan on 5/24/17.
 */
public interface Extractor<E> {
    List<E> extract(PaperInfo paperInfo);
}
