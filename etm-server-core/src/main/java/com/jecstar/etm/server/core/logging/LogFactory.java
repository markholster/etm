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

package com.jecstar.etm.server.core.logging;

import java.util.Collections;
import java.util.List;

/**
 * Class that produces <code>LogWrapper</code>'s. This factory is the entry
 * point for getting a logger to write log statements to.
 *
 * @author Mark Holster
 */
public class LogFactory {

    /**
     * Gives a configured <code>LogWrapper</code>.
     *
     * @param loggerName The name that the internal logger should have.
     * @return A <code>LogWrapper</code> instance.
     */
    private static LogWrapper getLogger(String loggerName) {
        return new Slf4jLogWrapper(loggerName);
    }

    /**
     * Gives a configured <code>LogWrapper</code>.
     *
     * @param loggerClass The class that is going to use the <code>LogWrapper</code>.
     * @return A <code>LogWrapper</code> instance.
     */
    public static LogWrapper getLogger(Class<?> loggerClass) {
        return getLogger(loggerClass.getName());
    }

    /**
     * Gives a <code>List</code> with currently known logger names. This works
     * only with managed loggers.
     *
     * @return A <code>List</code> with currently known logger names.
     */
    public static List<String> getCurrentLoggerNames() {
        return Collections.emptyList();
    }

}
