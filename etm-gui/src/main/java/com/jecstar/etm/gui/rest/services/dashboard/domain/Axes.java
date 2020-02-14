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

package com.jecstar.etm.gui.rest.services.dashboard.domain;

import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.XAxisConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.YAxisConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.XAxis;
import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.YAxis;
import com.jecstar.etm.server.core.converter.JsonField;

public class Axes {

    private static final String X_AXIS = "x_axis";
    private static final String Y_AXIS = "y_axis";

    @JsonField(value = X_AXIS, converterClass = XAxisConverter.class)
    private XAxis xAxis;

    @JsonField(value = Y_AXIS, converterClass = YAxisConverter.class)
    private YAxis yAxis;

    public XAxis getXAxis() {
        return this.xAxis;
    }

    public YAxis getYAxis() {
        return this.yAxis;
    }

}
