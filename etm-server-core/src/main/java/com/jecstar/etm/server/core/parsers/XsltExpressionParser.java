package com.jecstar.etm.server.core.parsers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.om.NamePool.NamePoolLimitException;

public class XsltExpressionParser implements ExpressionParser {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(XsltExpressionParser.class);
	
	private Transformer transformer;
	private final String template;
	
	public XsltExpressionParser(String template) {
	    this.transformer = createTransformer(template);
		this.template = template;
    }
	
	private Transformer createTransformer(String template) {
		try {
			Configuration config = Configuration.newConfiguration();
			config.setErrorListener(new XmlErrorListener());
			TransformerFactory transformerFactory = new TransformerFactoryImpl(config);
	        return transformerFactory.newTransformer(new StreamSource(new StringReader(template)));
        } catch (TransformerConfigurationException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Error creating xslt template from '" + template + ".", e);
        	}
	        throw new EtmException(EtmException.INVALID_XSLT_TEMPLATE, e);
        }		
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
        } catch (NamePoolLimitException e) {
        	this.transformer = createTransformer(this.template);
    	    try {
    	        this.transformer.transform(new StreamSource(new StringReader(content)), new StreamResult(writer));
    	        return writer.toString();
            } catch (TransformerException e2) {
            	if (log.isDebugLevelEnabled()) {
            		log.logDebugMessage("XSLT template '" + this.template+ "' could not be applied on content '" + content + "'.", e2);
            	}
            	return null;
            }        	
		}
    }

	public String getTemplate() {
	    return this.template;
    }

}
