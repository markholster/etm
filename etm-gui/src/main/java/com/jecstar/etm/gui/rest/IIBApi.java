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

package com.jecstar.etm.gui.rest;

public class IIBApi {

    public static boolean IIB_PROXY_ON_CLASSPATH;
    public static boolean IIB_V10_ON_CLASSPATH;

    static {
        try {
            Class.forName("com.ibm.broker.config.proxy.BrokerProxy");
            IIB_PROXY_ON_CLASSPATH = true;
        } catch (ClassNotFoundException e) {
            IIB_PROXY_ON_CLASSPATH = false;
        }
        try {
            Class.forName("com.ibm.broker.config.proxy.SubFlowProxy");
            IIB_V10_ON_CLASSPATH = true;
        } catch (ClassNotFoundException e) {
            IIB_V10_ON_CLASSPATH = false;
        }

    }
}
