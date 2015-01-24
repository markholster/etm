package com.holster.etm.processor.parsers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;

public class XsltExpressionParser implements ExpressionParser {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(XsltExpressionParser.class);
	
	private final Transformer transformer;
	private final String template;
	
	public XsltExpressionParser(TransformerFactory transformerFactory, String template) throws TransformerConfigurationException {
		this.transformer = transformerFactory.newTransformer(new StreamSource(new StringReader(template)));
		this.template = template;
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
        	if (log.isDebugLevelEnabled()) {
        		log.logDebugMessage("XSLT template '" + this.template+ "' could not be applied on content '" + content + "'.", e);
        	}
        	return null;
        }
    }

}
