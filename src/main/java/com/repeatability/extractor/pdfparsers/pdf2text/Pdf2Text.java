package com.repeatability.extractor.pdfparsers.pdf2text;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.Normalizer;
import java.util.Scanner;

import com.repeatability.extractor.util.Utf8;

/**
 * Created by savan on 6/6/17.
 */
public class Pdf2Text {
    private String text;

    public String getText() {
        return text;
    }

//    @SuppressWarnings("Since15")
    public void loadText(String filename) throws FileNotFoundException {
        try (Scanner scanner = Utf8.newScanner(new File(filename))) {
            text = scanner.useDelimiter("\\Z").next();
        }
        text = Normalizer.normalize(text, Normalizer.Form.NFKC);
    }
}
