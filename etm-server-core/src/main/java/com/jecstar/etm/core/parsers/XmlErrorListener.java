package com.jecstar.etm.core.parsers;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;

public class XmlErrorListener implements ErrorListener {

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
