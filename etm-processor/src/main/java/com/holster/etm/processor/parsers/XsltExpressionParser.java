package com.holster.etm.processor.parsers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XsltExpressionParser implements ExpressionParser {
	
	private final Transformer transformer;
	
	public XsltExpressionParser(TransformerFactory transformerFactory, String xslt) throws TransformerConfigurationException {
		this.transformer = transformerFactory.newTransformer(new StreamSource(new StringReader(xslt)));
    }

	@Override
    public String evaluate(String content) {
		if (this.transformer == null) {
			return null;
		}
		StringWriter writer = new StringWriter();
	    try {
	        this.transformer.transform(new StreamSource(new StringReader(content)), new StreamResult(writer));
	        return writer.toString();
        } catch (TransformerException e) {
        	// TODO logging
        	return null;
        }
    }

}
