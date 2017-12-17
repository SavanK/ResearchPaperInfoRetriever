package com.repeatability.extractor.pdfparsers.pdf2xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by savan on 5/29/17.
 */
@XmlRootElement (name="pdf2xml")
public class Pdf2Xml {
    private String producer;
    private String version;
    private List<Page> pages;

    public Pdf2Xml() {
    }

    public Pdf2Xml(String producer, String version, List<Page> pages) {
        super();
        this.producer = producer;
        this.version = version;
        this.pages = pages;
    }

    public String getProducer() {
        return producer;
    }

    @XmlAttribute (name="producer")
    public void setProducer(String producer) {
        this.producer = producer;
    }

    public String getVersion() {
        return version;
    }

    @XmlAttribute (name="version")
    public void setVersion(String version) {
        this.version = version;
    }

    public List<Page> getPages() {
        return pages;
    }

    @XmlElement (name="page")
    public void setPages(List<Page> pages) {
        this.pages = pages;
    }
}
