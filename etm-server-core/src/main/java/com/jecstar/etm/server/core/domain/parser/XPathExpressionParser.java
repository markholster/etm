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

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.NamePool.NamePoolLimitException;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.StringReader;

public class XPathExpressionParser extends AbstractExpressionParser {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(XPathExpressionParser.class);

    private XPathExpression compiledExpression;
    private final String expression;

    public XPathExpressionParser(final String name, final String expression) {
        super(name);
        this.compiledExpression = createCompiledExpression(expression);
        this.expression = expression;
    }

    private XPathExpression createCompiledExpression(String expression) {
        try {
            Configuration config = Configuration.newConfiguration();
            config.setErrorListener(new XmlErrorListener());
            config.setSchemaValidationMode(Validation.STRIP);
            config.setValidation(false);
            config.getParseOptions().addParserFeature("http://xml.org/sax/features/validation", false);
            config.getParseOptions().addParserFeature("http://apache.org/xml/features/validation/schema", false);
            config.getParseOptions().addParserFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            config.getParseOptions().addParserFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            config.getParseOptions().addParserFeature("http://xml.org/sax/features/external-general-entities", false);
            config.getParseOptions().addParserFeature("http://xml.org/sax/features/external-parameter-entities", false);
            XPath xPath = new XPathFactoryImpl(config).newXPath();
            return xPath.compile(expression);
        } catch (XPathExpressionException e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Error creating xpath expression from '" + expression + ".", e);
            }
            throw new EtmException(EtmException.INVALID_XPATH_EXPRESSION, e);
        }
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
        } catch (NamePoolLimitException e) {
            this.compiledExpression = createCompiledExpression(this.expression);
            try {
                return this.compiledExpression.evaluate(new InputSource(new StringReader(content)));
            } catch (XPathExpressionException e2) {
                if (log.isDebugLevelEnabled()) {
                    log.logDebugMessage("XPath expression '" + this.expression + "' could not be evaluated against content '" + content + "'.", e2);
                }
                return null;
            }
        }
    }

    public String getExpression() {
        return this.expression;
    }

}
