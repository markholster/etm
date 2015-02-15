package com.holster.etm.core.parsers;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.xpath.XPathFactoryImpl;

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

public final class ExpressionParserFactory {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ExpressionParserFactory.class);

	
	private static final XPath xPath = new XPathFactoryImpl().newXPath();
	private static final TransformerFactory transformerFactory = new TransformerFactoryImpl();


	public static ExpressionParser createExpressionParserFromConfiguration(final String expression) {
		if (expression.length() > 5) {
			if (expression.charAt(0) == 'x' &&
					expression.charAt(1) == 's' &&
					expression.charAt(2) == 'l' &&
					expression.charAt(3) == 't' &&
					expression.charAt(4) == ':') {
				try {
	                return new XsltExpressionParser(transformerFactory, expression.substring(5));
                } catch (TransformerConfigurationException e) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create XsltExpressionParser. Using FixedValueExpressionParser instead.", e);
                	}
                	new FixedValueExpressionParser(null);
                }				
			}
		}
		if (expression.length() > 6) {
			if (expression.charAt(0) == 'x' &&
				expression.charAt(1) == 'p' &&
				expression.charAt(2) == 'a' &&
				expression.charAt(3) == 't' &&
				expression.charAt(4) == 'h' &&
				expression.charAt(5) == ':') {
				try {
	                return new XPathExpressionParser(xPath, expression.substring(6));
                } catch (XPathExpressionException e) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create XPathExpressionParser. Using FixedValueExpressionParser instead.", e);
                	}
                	new FixedValueExpressionParser(null);
                }
			} else if (expression.charAt(0) == 'f' &&
					   expression.charAt(1) == 'i' &&
					   expression.charAt(2) == 'x' &&
					   expression.charAt(3) == 'e' &&
					   expression.charAt(4) == 'd' &&
					   expression.charAt(5) == ':') {
				String range = expression.substring(6);
				String[] values = range.split("-", 3);
				if (values.length != 3) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create FixedPositionExpressionParser. Range '" + range + "' is invalid. Using FixedValueExpressionParser instead.");
                	}
                	new FixedValueExpressionParser(null);					
				}
				try {
					Integer line = null;
					Integer start = null;
					Integer end = null;
					if (values[0].trim().length() != 0) {
						line = Integer.valueOf(values[0].trim());
					}
					if (values[1].trim().length() != 0) {
						start = Integer.valueOf(values[1]);
					} 
					if (values[2].trim().length() != 0) {
						end = Integer.valueOf(values[2]);
					} 
					return new FixedPositionExpressionParser(line, start, end);
				} catch (NumberFormatException e) {
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Could not create FixedPositionExpressionParser. Range '" + range + "' is invalid. Using FixedValueExpressionParser instead.", e);
                	}
                	new FixedValueExpressionParser(null);										
				}
			}
		}
		return new FixedValueExpressionParser(expression);
	}
}
