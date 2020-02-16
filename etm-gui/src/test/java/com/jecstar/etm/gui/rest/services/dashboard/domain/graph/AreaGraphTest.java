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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the <code>AreaGraph</code> class.
 */
public class AreaGraphTest {

    @Test
    public void testMergeFromColumn() {
        var areaGraph = new AreaGraph();
        areaGraph.setLineType(LineType.STEP_RIGHT).setOrientation(AxesGraph.Orientation.VERTICAL);

        var mergedGraph = new AreaGraph().mergeFromColumn(areaGraph);

        assertEquals(areaGraph.getSubType(), mergedGraph.getSubType());
        assertEquals(areaGraph.getOrientation(), mergedGraph.getOrientation());

        var mergedLineGraph = new LineGraph().mergeFromColumn(areaGraph);
        assertEquals(areaGraph.getOrientation(), mergedLineGraph.getOrientation());
    }
}
