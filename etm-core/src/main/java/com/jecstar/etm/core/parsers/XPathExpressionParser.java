package com.jecstar.etm.core.parsers;

import java.io.StringReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.InputSource;

import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

public class XPathExpressionParser implements ExpressionParser {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(XPathExpressionParser.class);
	
	private final XPathExpression compiledExpression;
	private final String expression;
	
	public XPathExpressionParser(XPath xPath, String expression) {
		try {
	        this.compiledExpression = xPath.compile(expression);
        } catch (XPathExpressionException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error creating xpath expression from '" + expression + ".", e);
        	}
	        throw new EtmException(EtmException.INVALID_XPATH_EXPRESSION, e);
        }
		this.expression = expression;
    }

	@Override
    public String evaluate(String content) {
		if (this.compiledExpression == null) {
			return null;
		}
	    try {
	        return this.compiledExpression.evaluate(new InputSource(new StringReader(content)));
        } catch (XPathExpressionException e) {
        	if (log.isDebugLevelEnabled()) {
        		log.logDebugMessage("XPath expression '" + this.expression + "' could not be evaluated against content '" + content + "'.", e);
        	}
	        return null;
        }
    }

	public String getExpression() {
	    return this.expression;
    }

}
