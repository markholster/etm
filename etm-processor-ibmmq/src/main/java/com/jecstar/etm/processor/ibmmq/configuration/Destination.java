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

package com.jecstar.etm.processor.ibmmq.configuration;

import com.ibm.mq.constants.CMQC;

public class Destination {

    public static final int DEFAULT_GET_OPTIONS = CMQC.MQGMO_WAIT + CMQC.MQGMO_SYNCPOINT + CMQC.MQGMO_ACCEPT_TRUNCATED_MSG + CMQC.MQGMO_FAIL_IF_QUIESCING + CMQC.MQGMO_LOGICAL_ORDER + CMQC.MQGMO_COMPLETE_MSG + CMQC.MQGMO_ALL_SEGMENTS_AVAILABLE;

    private String name;
    private String type = "queue";
    private int minNrOfListeners = 1;
    private int maxNrOfListeners = 5;
    private String messagesType = "auto"; // iibevent, etmevent, clone
    private String defaultImportProfile;

    private int maxMessageSize = 1024 * 1024 * 4;
    private int commitSize = 500;
    private int commitInterval = 10000;
    private int destinationGetOptions = DEFAULT_GET_OPTIONS;
    private int destinationOpenOptions = CMQC.MQOO_INQUIRE + CMQC.MQOO_FAIL_IF_QUIESCING + CMQC.MQOO_INPUT_SHARED;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        if (!"queue".equalsIgnoreCase(type) && !"topic".equalsIgnoreCase(type)) {
            throw new IllegalArgumentException("'" + type + "' is an invalid destination type.");
        }
        this.type = type;
    }

    public int getMinNrOfListeners() {
        if ("topic".equalsIgnoreCase(type)) {
            return 1;
        }
        return this.minNrOfListeners;
    }

    public void setMinNrOfListeners(int minNrOfListeners) {
        if (minNrOfListeners < 1 || minNrOfListeners > 65535) {
            throw new IllegalArgumentException(minNrOfListeners + " is an invalid minimum number of listeners");
        }
        this.minNrOfListeners = minNrOfListeners;
    }

    public int getMaxNrOfListeners() {
        if ("topic".equalsIgnoreCase(type)) {
            return 1;
        }
        return this.maxNrOfListeners;
    }

    public void setMaxNrOfListeners(int maxNrOfListeners) {
        if (maxNrOfListeners < 1 || maxNrOfListeners > 65535) {
            throw new IllegalArgumentException(maxNrOfListeners + " is an invalid maximum number of listeners");
        }
        this.maxNrOfListeners = maxNrOfListeners;
    }

    public String getMessagesType() {
        return this.messagesType;
    }

    public void setMessagesType(String messagesType) {
        if (!"auto".equalsIgnoreCase(messagesType)
                && !"iibevent".equalsIgnoreCase(messagesType)
                && !"etmevent".equalsIgnoreCase(messagesType)
                && !"clone".equalsIgnoreCase(messagesType)) {
            throw new IllegalArgumentException("'" + messagesType + "' is an invalid messages type.");
        }
        this.messagesType = messagesType;
    }

    public int getMaxMessageSize() {
        return this.maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        if (maxMessageSize < 1) {
            throw new IllegalArgumentException(maxMessageSize + " is an invalid max message size");
        }
        this.maxMessageSize = maxMessageSize;
    }

    public String getDefaultImportProfile() {
        return this.defaultImportProfile;
    }

    public void setDefaultImportProfile(String defaultImportProfile) {
        this.defaultImportProfile = defaultImportProfile;
    }

    public int getCommitSize() {
        return this.commitSize;
    }

    public void setCommitSize(int commitSize) {
        if (commitSize < 0) {
            throw new IllegalArgumentException(commitSize + " is an invalid commit size");
        }
        this.commitSize = commitSize;
    }

    public int getDestinationGetOptions() {
        return this.destinationGetOptions;
    }

    public int getCommitInterval() {
        return this.commitInterval;
    }

    public void setCommitInterval(int commitInterval) {
        if (commitInterval < 0) {
            throw new IllegalArgumentException(commitInterval + " is an invalid commit interval");
        }
        this.commitInterval = commitInterval;
    }

    public void setDestinationGetOptions(int destinationGetOptions) {
        if (destinationGetOptions < 0) {
            throw new IllegalArgumentException(destinationGetOptions + " is an invalid destination get option number");
        }
        this.destinationGetOptions = destinationGetOptions;
    }

    public int getDestinationOpenOptions() {
        return this.destinationOpenOptions;
    }

    public void setDestinationOpenOptions(int destinationOpenOptions) {
        if (destinationOpenOptions < 0) {
            throw new IllegalArgumentException(destinationOpenOptions + " is an invalid destination open option number");
        }
        this.destinationOpenOptions = destinationOpenOptions;
    }
}
