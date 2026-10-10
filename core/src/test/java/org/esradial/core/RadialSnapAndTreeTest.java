package org.esradial.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RadialSnapAndTreeTest {
    @Test void nearestSnapUsesTheUnionNotEveryFifteenDegrees() {
        assertEquals(30, RadialLayoutEditor.snapAngle(33, RadialLayoutEditor.Snap.BOTH));
        assertEquals(45, RadialLayoutEditor.snapAngle(43, RadialLayoutEditor.Snap.BOTH));
        assertEquals(90, RadialLayoutEditor.snapAngle(76, RadialLayoutEditor.Snap.BOTH));
        assertEquals(0, RadialLayoutEditor.snapAngle(359, RadialLayoutEditor.Snap.BOTH));
        assertEquals(330, RadialLayoutEditor.snapAngle(-29, RadialLayoutEditor.Snap.BOTH));
        assertEquals(30, RadialLayoutEditor.snapAngle(43, RadialLayoutEditor.Snap.THIRTY));
        assertEquals(45, RadialLayoutEditor.snapAngle(33, RadialLayoutEditor.Snap.FORTY_FIVE));
    }
    @Test void draggingEveryBoundaryKeepsLegalGridAndCoverage() {
        for (var snap : List.of(RadialLayoutEditor.Snap.BOTH, RadialLayoutEditor.Snap.THIRTY, RadialLayoutEditor.Snap.FORTY_FIVE)) {
            var editor = new RadialLayoutEditor(RadialLayout.squad(44,96,6),6);
            for (int i=0;i<8;i++) for (int angle=0;angle<360;angle+=13) {
                var before = editor.layout();
                editor.resizeBoundary(i, angle, snap);
                var after=editor.layout(); after.validateSlots(6);
                assertEquals(360,after.sectors().stream().mapToDouble(RadialLayout.Sector::sweepDegrees).sum(),1e-8);
                double current=after.sectors().get(i).startDegrees();
                if (Math.abs(current-before.sectors().get(i).startDegrees())>1e-8)
                    assertEquals(current,RadialLayoutEditor.snapAngle(current,snap),1e-8);
            }
        }
    }
    @Test void scrollingGoesToNextGridStopAndWrapsBothWays() {
        var editor = new RadialLayoutEditor(new RadialLayout(44,96,List.of(new RadialLayout.Sector(0,360,0))),1);
        editor.rotateSnapped(1,RadialLayoutEditor.Snap.BOTH);assertEquals(30,editor.layout().sectors().get(0).startDegrees());
        editor.rotateSnapped(1,RadialLayoutEditor.Snap.BOTH);assertEquals(45,editor.layout().sectors().get(0).startDegrees());
        editor.rotateSnapped(-1,RadialLayoutEditor.Snap.BOTH);assertEquals(30,editor.layout().sectors().get(0).startDegrees());
        editor.rotateSnapped(-1,RadialLayoutEditor.Snap.BOTH);
        editor.rotateSnapped(-1,RadialLayoutEditor.Snap.BOTH);assertEquals(330,editor.layout().sectors().get(0).startDegrees());
    }
    @Test void defaultSmallTreesUseSnappedBoundariesAndTwoEmptySectors() {
        for(int n=1;n<=9;n++) {
            var layout=RadialLayout.squad(44,96,n);layout.validateSlots(n);
            assertEquals(2,layout.sectors().stream().filter(s->s.slotIndex()<0).count());
            for(var sector:layout.sectors()) assertEquals(sector.startDegrees(),
                RadialLayoutEditor.snapAngle(sector.startDegrees(),RadialLayoutEditor.Snap.BOTH),1e-8);
        }
    }
    @Test void releasingOverDirectoryCancelsWithoutNavigationOrBusinessAction() {
        var calls=new AtomicInteger();
        var slot=new RadialSession.Slot<>("folder","folder",true,false,0,(Runnable)calls::incrementAndGet,true);
        var s=new RadialSession<>(new RadialSession.Page<>("root",new RadialLayout(44,96),List.of(slot)),r->{});
        s.hover(0,-70);assertFalse(s.confirmRelease());assertTrue(s.isClosed());assertEquals(0,calls.get());
    }
    @Test void heldClickCannotTriggerTheLeafAfterEnteringDirectory() {
        var calls=new AtomicInteger();
        var leaf=new RadialSession.Page<>("child",new RadialLayout(44,96),List.of(
            new RadialSession.Slot<>("leaf","leaf",true,true,0,(Runnable)calls::incrementAndGet)));
        @SuppressWarnings("unchecked") RadialSession<String>[] ref=new RadialSession[1];
        var folder=new RadialSession.Slot<>("folder","folder",true,false,0,(Runnable)()->ref[0].push(leaf),true);
        ref[0]=new RadialSession<>(new RadialSession.Page<>("root",new RadialLayout(44,96),List.of(folder)),r->{});
        var s=ref[0];s.hover(0,-70);s.updatePrimary(true);s.hover(0,-70);
        s.updatePrimary(true);s.updatePrimary(true);assertEquals(0,calls.get());
        assertTrue(s.back());assertEquals("root",s.page().id());
        s.updatePrimary(false);s.hover(0,-70);s.updatePrimary(true);s.updatePrimary(false);
        s.hover(0,-70);s.updatePrimary(true);assertEquals(1,calls.get());
    }
}
