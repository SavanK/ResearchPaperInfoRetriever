/* ***************************************************************************
$Header: //RepeatabilityDepot/repeatability/Java/Repeatability/src/com/repeatability/util/Encoder.java#3 $
******************************************************************************
Package
*****************************************************************************/
package com.repeatability.extractor.util;
/* ***************************************************************************
Imports
*****************************************************************************/
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
/* ***************************************************************************
Class
*****************************************************************************/
public class Utf8 {
	public static final String UTF8 = StandardCharsets.UTF_8.toString();
	
	protected static class LocalByteArrayOutputStream extends ByteArrayOutputStream {
		
		@Override
		public String toString() {
			try {
				return super.toString(Utf8.UTF8);
			}
			catch (UnsupportedEncodingException exception) {
				return super.toString();
			}
		}
	}
	
	public static OutputStreamWriter newOutputStreamWriter(OutputStream outputStream) throws UnsupportedEncodingException {
		return new OutputStreamWriter(outputStream, Utf8.UTF8);
	}
	
	public static InputStreamReader newInputStreamReader(InputStream inputStream) throws UnsupportedEncodingException {
		return new InputStreamReader(inputStream, Utf8.UTF8);
	}
	
	public static XMLStreamReader newXMLStreamReader(XMLInputFactory xmlInputFactry, InputStream inputStream) throws XMLStreamException {
		return xmlInputFactry.createXMLStreamReader(inputStream, Utf8.UTF8);
	}
	
	public static ByteArrayOutputStream newByteArrayOutputStream() {
		return new LocalByteArrayOutputStream();
	}
	
	public static Scanner newScanner(File file) throws FileNotFoundException {
		return new Scanner(file, Utf8.UTF8);
	}
}
/* **************************************************************************/
