package com.jecstar.etm.server.core.domain.parser;

import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

class XmlErrorListener implements ErrorListener {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(XmlErrorListener.class);

    @Override
    public void warning(TransformerException exception) {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage(exception.getMessage(), exception);
        }
    }

    @Override
    public void error(TransformerException exception) {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage(exception.getMessage(), exception);
        }
    }

    @Override
    public void fatalError(TransformerException exception) {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage(exception.getMessage(), exception);
        }
    }

}
