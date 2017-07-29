package com.jecstar.etm.server.core.domain.parser;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

class XmlErrorListener implements ErrorListener {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(XmlErrorListener.class);
	
	@Override
    public void warning(TransformerException exception) throws TransformerException {
	    if (log.isDebugLevelEnabled()) {
	    	log.logDebugMessage(exception.getMessage(), exception);
	    }
    }

	@Override
    public void error(TransformerException exception) throws TransformerException {
	    if (log.isDebugLevelEnabled()) {
	    	log.logDebugMessage(exception.getMessage(), exception);
	    }
    }

	@Override
    public void fatalError(TransformerException exception) throws TransformerException {
	    if (log.isDebugLevelEnabled()) {
	    	log.logDebugMessage(exception.getMessage(), exception);
	    }
    }

}
