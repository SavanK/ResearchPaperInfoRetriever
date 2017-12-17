package com.repeatability.extractor.pdfparsers.pdf2xml;

import com.repeatability.extractor.util.Point2D;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;
import java.text.Normalizer;

/**
 * Created by savan on 5/29/17.
 */
public class Text {
    private int top;
    private int left;
    private int height;
    private int width;
    private int font;
    private String value;

    public Text() {
    }

    public Text(int top, int left, int height, int width, int font, String value) {
        super();
        this.top = top;
        this.left = left;
        this.height = height;
        this.width = width;
        this.font = font;
        this.value = value;
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

    public int getFont() {
        return font;
    }

    @XmlAttribute (name = "font")
    public void setFont(int font) {
        this.font = font;
    }

    public String getValue() {
        return value;
    }

    public Point2D getCentroid() {
        double x1 = left;
        double x2 = left + width;
        double y1 = top;
        double y2 = top + height;

        return new Point2D((x1+x2)/2.f, (y1+y2)/2.f);
    }

//    @SuppressWarnings("Since15")
    @XmlValue
    public void setValue(String value) {
        this.value = value;
        this.value = Normalizer.normalize(this.value, Normalizer.Form.NFKC);
    }
}
