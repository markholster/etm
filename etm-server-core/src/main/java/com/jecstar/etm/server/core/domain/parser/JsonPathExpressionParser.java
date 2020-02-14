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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class JsonPathExpressionParser extends AbstractExpressionParser {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(JsonPathExpressionParser.class);

    private final JsonPath expression;

    static {
        Configuration.setDefaults(new JsonPathDefaults());
    }


    public JsonPathExpressionParser(final String name, final String expression) {
        super(name);
        this.expression = JsonPath.compile(expression);
        if (!this.expression.isDefinite()) {
            throw new EtmException(EtmException.INVALID_JSON_EXPRESSION);
        }
    }

    @Override
    public String evaluate(String content) {
        if (this.expression == null) {
            return null;
        }
        try {
            return JsonPath.parse(content).read(this.expression, String.class);
        } catch (Exception e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Json expression '" + this.expression.getPath() + "' could not be evaluated against content '" + content + "'.", e);
            }
            return null;
        }
    }

    public String getExpression() {
        return this.expression.getPath();
    }

}
