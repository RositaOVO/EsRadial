package org.esradial.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RadialClockLayoutTest {
    @Test void actionsStartAtTwelveAndLeaveOnlyOneTrailingGap() {
        var layout = RadialClockLayout.create(44,96,6,5);
        for (int i=0;i<5;i++) {
            var area=layout.sectorForSlot(i,6);
            assertEquals(i*30,area.startDegrees()); assertEquals(30,area.sweepDegrees());
            assertEquals(i,layout.hitIndex(layout.slotX(i,6),layout.slotY(i,6),6));
        }
        var gaps=layout.sectors().stream().filter(s->s.slotIndex()<0).toList();
        assertEquals(1,gaps.size()); assertEquals(150,gaps.get(0).startDegrees());
        assertEquals(120,gaps.get(0).sweepDegrees());
        assertEquals(270,layout.sectorForSlot(5,6).startDegrees());
        assertEquals(90,layout.sectorForSlot(5,6).sweepDegrees());
    }
    @Test void nineActionsFillAllSpaceBeforeQuarterReturn() {
        var layout=RadialClockLayout.create(44,96,10,9);
        assertTrue(layout.sectors().stream().allMatch(s->s.slotIndex()>=0));
        assertEquals(270,layout.sectorForSlot(9,10).startDegrees());
        assertEquals(90,layout.sectorForSlot(9,10).sweepDegrees());
    }
    @Test void tenAndElevenActionsUseReturnQuarterWithoutOverlapping() {
        for(int actions=10;actions<=11;actions++) {
            var layout=RadialClockLayout.create(44,96,actions+1,actions);
            assertEquals(actions*30,layout.sectorForSlot(actions,actions+1).startDegrees());
            assertEquals(360-actions*30,layout.sectorForSlot(actions,actions+1).sweepDegrees());
            assertTrue(layout.sectors().stream().allMatch(s->s.slotIndex()>=0));
        }
    }
    @Test void guidesDoNotCreateTwelveActionSlots() {
        var layout=RadialClockLayout.create(44,96,2,-1);
        assertEquals(2,layout.sectors().stream().filter(s->s.slotIndex()>=0).count());
        assertEquals(-1,layout.hitIndex(0,70,2));
        assertEquals(-1,layout.hitIndex(0,0,2));
    }
}
