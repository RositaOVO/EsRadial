package org.esradial.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialViewSizingTest {
    @Test void navigationAndFooterStayVisibleAtAutomaticGuiScale() {
        for(double height:new double[]{240,255,382,600}) for(double radius:new double[]{64,96,108,160}) {
            double scale=RadialViewSizing.scale(radius,427,height),visible=radius*scale;
            assertTrue(height/2-visible-42>=0);
            assertTrue(height/2+visible+12+26+12<=height-5);
        }
    }
    @Test void ordinaryLargeViewportsKeepOriginalWheelSize() {
        assertEquals(1,RadialViewSizing.scale(96,640,382));
        assertTrue(RadialViewSizing.scale(96,427,255)<1);
    }
}
