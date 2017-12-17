package com.repeatability.extractor.pdfparsers.pdf2xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

/**
 * Created by savan on 5/29/17.
 */
public class Page {
    private int number;
    private String position;
    private int top;
    private int left;
    private int height;
    private int width;
    private List<FontSpec> fontSpecs;
    private List<Text> texts;

    public Page() {
    }

    public Page(int number, String position, int top, int left, int height, int width,
                List<FontSpec> fontSpecs, List<Text> texts) {
        super();
        this.number = number;
        this.position = position;
        this.top = top;
        this.left = left;
        this.height = height;
        this.width = width;
        this.fontSpecs = fontSpecs;
        this.texts = texts;
    }

    public int getNumber() {
        return number;
    }

    @XmlAttribute (name = "number")
    public void setNumber(int number) {
        this.number = number;
    }

    public String getPosition() {
        return position;
    }

    @XmlAttribute (name = "position")
    public void setPosition(String position) {
        this.position = position;
    }

    public int getTop() {
        return top;
    }

    @XmlAttribute (name = "top")
    public void setTop(int top) {
        this.top = top;
    }

    public int getLeft() {
        return left;
    }

    @XmlAttribute (name = "left")
    public void setLeft(int left) {
        this.left = left;
    }

    public int getHeight() {
        return height;
    }

    @XmlAttribute (name = "height")
    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    @XmlAttribute (name = "width")
    public void setWidth(int width) {
        this.width = width;
    }

    public List<FontSpec> getFontSpecs() {
        return fontSpecs;
    }

    @XmlElement (name = "fontspec")
    public void setFontSpecs(List<FontSpec> fontSpecs) {
        this.fontSpecs = fontSpecs;
    }

    public List<Text> getTexts() {
        return texts;
    }

    @XmlElement (name = "text")
    public void setTexts(List<Text> texts) {
        this.texts = texts;
    }
}
