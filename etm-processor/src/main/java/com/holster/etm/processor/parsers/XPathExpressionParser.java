package com.holster.etm.processor.parsers;

import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.InputSource;

public class XPathExpressionParser implements ExpressionParser {
	
	private final XPathExpression compiledExpression;
	
	public XPathExpressionParser(XPath xPath, String expression) throws XPathExpressionException {
		this.compiledExpression = xPath.compile(expression);
    }

	@Override
    public String evaluate(String content) {
		if (this.compiledExpression == null) {
			return null;
		}
	    try {
	        return this.compiledExpression.evaluate(new InputSource(new StringReader(content)));
        } catch (XPathExpressionException e) {
        	// TODO logging
	        return null;
        }
    }

}
