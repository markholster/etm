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

package com.jecstar.etm.gui.rest.services.iib;

public class NodeTagsJsonImpl implements NodeTags {

    @Override
    public String getNameTag() {
        return "name";
    }

    @Override
    public String getHostTag() {
        return "host";
    }

    @Override
    public String getPortTag() {
        return "port";
    }

    @Override
    public String getUsernameTag() {
        return "username";
    }

    @Override
    public String getPasswordTag() {
        return "password";
    }

    @Override
    public String getQueueManagerTag() {
        return "queue_manager";
    }

    @Override
    public String getChannelTag() {
        return "channel";
    }

}
