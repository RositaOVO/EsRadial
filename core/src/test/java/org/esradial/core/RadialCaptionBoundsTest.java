package org.esradial.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialCaptionBoundsTest {
    @Test void captionsNeverCrossTheRingOrNeighboringSectors() {
        for (int count=1;count<=9;count++) {
            var layout=RadialLayout.squad(44,96,count);
            for(int slot=0;slot<count;slot++) for(double scale:new double[]{.75,.9,1}) {
                var b=RadialCaptionBounds.forSlot(layout,slot,count,scale);
                if(b.width()==0)continue;
                for(double x:new double[]{b.left(),b.left()+b.width()}) for(double y:new double[]{b.top(),b.top()+b.height()}) {
                    assertTrue(Math.hypot(x,y)<=96*scale-3+1e-9);
                    assertEquals(slot,layout.hitIndex(x/scale,y/scale,count));
                }
                double x=Math.max(b.left(),Math.min(0,b.left()+b.width()));
                double y=Math.max(b.top(),Math.min(0,b.top()+b.height()));
                assertTrue(Math.hypot(x,y)>=44*scale+2);
            }
        }
    }
    @Test void sideCaptionsAreNarrowerThanTheOldFixedWidth() {
        var layout=new RadialLayout(44,96);
        var b=RadialCaptionBounds.forSlot(layout,1,4,1);
        assertTrue(b.width()>0 && b.width()<72);
    }
}
