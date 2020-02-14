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

package com.jecstar.etm.gui.rest.services.dashboard.domain.graph;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

public enum LineType {

    STRAIGHT, SMOOTH, STEP_LEFT, STEP_CENTER, STEP_RIGHT;

    public static LineType safeValueOf(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LineType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getHighchartsChartType(AxesGraph axesGraph) {
        if (axesGraph instanceof LineGraph) {
            return SMOOTH.equals(this) ? "spline" : "line";
        } else if (axesGraph instanceof AreaGraph) {
            return SMOOTH.equals(this) ? "areaspline" : "area";
        }
        throw new IllegalArgumentException("Unsupported graph: " + axesGraph.getClass().getName());
    }

    public void addHighchartsPlotOptions(JsonBuilder builder) {
        if (STEP_LEFT.equals(this)) {
            builder.field("step", "left");
        } else if (STEP_CENTER.equals(this)) {
            builder.field("step", "center");
        } else if (STEP_RIGHT.equals(this)) {
            builder.field("step", "right");
        }
    }
}
