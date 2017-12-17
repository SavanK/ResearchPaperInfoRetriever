package com.repeatability.extractor.pdfparsers.pdf2xml;

import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.DomHandler;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by savan on 5/29/17.
 */
public class TextHandler implements DomHandler<String, StreamResult> {
    private static final String TEXT_START_TAG = "<text>";
    private static final String TEXT_END_TAG = "</text>";
    private StringWriter xmlWriter = new StringWriter();

    @Override
    public StreamResult createUnmarshaller(ValidationEventHandler errorHandler) {
        return new StreamResult(xmlWriter);
    }

    @Override
    public String getElement(StreamResult rt) {
        String xml = rt.getWriter().toString();
        int beginIndex = xml.indexOf(TEXT_START_TAG) + TEXT_START_TAG.length();
        int endIndex = xml.indexOf(TEXT_END_TAG);
        return xml.substring(beginIndex, endIndex);
    }

    @Override
    public Source marshal(String n, ValidationEventHandler errorHandler) {
        try {
            String xml = TEXT_START_TAG + n.trim() + TEXT_END_TAG;
            StringReader xmlReader = new StringReader(xml);
            return new StreamSource(xmlReader);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
