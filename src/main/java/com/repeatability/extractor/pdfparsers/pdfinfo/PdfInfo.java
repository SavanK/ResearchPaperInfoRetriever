package com.repeatability.extractor.pdfparsers.pdfinfo;

/**
 * Created by savan on 5/29/17.
 */
public class PdfInfo {
    private String title;
    private String creator;
    private String producer;

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getProducer() {
        return producer;
    }

    public void parse(String pdfInfoStr) {
        String[] lines = pdfInfoStr.split("\n");
        for (String line : lines) {
            if(line.startsWith("Title")) {
                title = line.substring(line.indexOf(':')+1).trim();
            } else if(line.startsWith("Creator")) {
                creator = line.substring(line.indexOf(':')+1).trim();
            } else if(line.startsWith("Producer")) {
                producer = line.substring(line.indexOf(':')+1).trim();
            }
        }
    }
}
