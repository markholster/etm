package com.jecstar.etm.core.parsers;

import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.xpath.XPathFactoryImpl;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

public final class ExpressionParserFactory {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ExpressionParserFactory.class);
	
	private static final XPath xPath;
	private static final TransformerFactory transformerFactory;

	static {
		Configuration config = Configuration.newConfiguration();
		config.setErrorListener(new XmlErrorListener());
		xPath = new XPathFactoryImpl(config).newXPath();
		transformerFactory = new TransformerFactoryImpl(config);
	}

	public static ExpressionParser createExpressionParserFromConfiguration(final String expression) {
		if (expression.length() > 5) {
			if (expression.charAt(0) == 'x' &&
				expression.charAt(1) == 's' &&
				expression.charAt(2) == 'l' &&
				expression.charAt(3) == 't' &&
				expression.charAt(4) == ':') {
				try {
					return new XsltExpressionParser(transformerFactory, expression.substring(5));
				} catch (EtmException e) {
					new FixedValueExpressionParser(null);
				}
			} else if (expression.charAt(0) == 'j' &&
					   expression.charAt(1) == 's' &&
					   expression.charAt(2) == 'o' &&
					   expression.charAt(3) == 'n' &&
					   expression.charAt(4) == ':') {
				try {
					return new JsonExpressionParser(expression.substring(5));
				} catch (EtmException e) {
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
				} catch (EtmException e) {
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
	
	public static String toConfiguration(ExpressionParser expressionParser) {
		if (expressionParser instanceof XsltExpressionParser) {
			return "xslt:" + ((XsltExpressionParser)expressionParser).getTemplate();
		} else if (expressionParser instanceof XPathExpressionParser) {
			return "xpath:" + ((XPathExpressionParser)expressionParser).getExpression();
		} else if (expressionParser instanceof JsonExpressionParser) {
			return "json:" + ((JsonExpressionParser)expressionParser).getPath();
		} else if (expressionParser instanceof FixedPositionExpressionParser) {
			FixedPositionExpressionParser parser = (FixedPositionExpressionParser) expressionParser;
			String config = "fixed:";
			if (parser.getLineIx() != null) {
				config += parser.getLineIx(); 
			}
			config += "-";
			if (parser.getStartIx() != null) {
				config += parser.getStartIx();
			}
			config += "-";
			if (parser.getEndIx() != null) {
				config += parser.getEndIx();
			}
			return  config;
		} else if (expressionParser instanceof FixedValueExpressionParser) {
			return ((FixedValueExpressionParser)expressionParser).getValue();
		} else {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unknown expression parser type: '" + expressionParser.getClass().getName() + "'.");
			}
			throw new EtmException(EtmException.INVALID_EXPRESSION_PARSER_TYPE);
		}
		
	}
}
