/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

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
