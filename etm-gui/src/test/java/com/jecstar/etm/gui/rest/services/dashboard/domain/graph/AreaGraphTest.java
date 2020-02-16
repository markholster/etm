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
