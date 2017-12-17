package com.repeatability.extractor.pdfparsers.pdf2xml;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Created by savan on 5/29/17.
 */
public class FontSpec {
    private int id;
    private int size;
    private String family;
    private String color;

    public FontSpec() {
    }

    public FontSpec(int id, int size, String family, String color) {
        super();
        this.id = id;
        this.size = size;
        this.family = family;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    @XmlAttribute (name="id")
    public void setId(int id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    @XmlAttribute (name="size")
    public void setSize(int size) {
        this.size = size;
    }

    public String getFamily() {
        return family;
    }

    @XmlAttribute (name="family")
    public void setFamily(String family) {
        this.family = family;
    }

    public String getColor() {
        return color;
    }

    @XmlAttribute (name="color")
    public void setColor(String color) {
        this.color = color;
    }
}
