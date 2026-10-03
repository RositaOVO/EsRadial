package org.esradial.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RadialLayoutTest {
    private RadialLayout layout() {
        return new RadialLayout(44, 100, List.of(new RadialLayout.Sector(-30, 55, 0),
                new RadialLayout.Sector(25, 45, 1), new RadialLayout.Sector(170, 35, 2)));
    }
    private int hit(RadialLayout layout, double angle) {
        double radians = Math.toRadians(angle);
        return layout.hitIndex(Math.sin(radians) * 1000, -Math.cos(radians) * 1000, 3);
    }
    @Test void unequalSectorsHitTheirDisplayedIconsIncludingAcrossZero() {
        var layout = layout(); layout.validateSlots(3);
        for (int i = 0; i < 3; i++)
            assertEquals(i, layout.hitIndex(layout.slotX(i, 3), layout.slotY(i, 3), 3));
        assertEquals(0, hit(layout, 359)); assertEquals(0, hit(layout, 20));
        assertEquals(1, hit(layout, 60)); assertEquals(2, hit(layout, 190));
        assertEquals(-1, hit(layout, 120)); assertEquals(-1, hit(layout, 300));
        assertEquals(360, layout.sectors().stream().mapToDouble(RadialLayout.Sector::sweepDegrees).sum(), 1e-9);
    }
    @Test void sectorEndIsExclusiveAndSharedBoundaryBelongsToNextSector() {
        var first = new RadialLayout.Sector(-30, 55, 0);
        var second = new RadialLayout.Sector(25, 45, 1);
        assertTrue(first.contains(330)); assertTrue(first.contains(24.99));
        assertFalse(first.contains(25)); assertTrue(second.contains(25)); assertFalse(second.contains(70));
    }
    @Test void blankSectorsNeverExecuteAndStopHeldRepeatsImmediately() {
        AtomicInteger actions = new AtomicInteger();
        var geometry = new RadialLayout(44, 100, List.of(new RadialLayout.Sector(-30, 60, 0),
                new RadialLayout.Sector(30, 90, -1)));
        var session = new RadialSession<>(new RadialSession.Page<>("test", geometry,
                List.of(new RadialSession.Slot<>("action", "action", true, false, 2, actions::incrementAndGet))), reason -> {});
        session.hover(0, -80); session.updatePrimary(true); session.updatePrimary(true);
        session.hover(80, 0); assertEquals(-1, session.hoveredIndex());
        for (int i = 0; i < 20; i++) session.updatePrimary(true);
        assertEquals(1, actions.get()); assertEquals(0, session.repeatProgress());
        session.updatePrimary(false); session.updatePrimary(true);
        assertFalse(session.confirmRelease()); assertEquals(1, actions.get());
    }
    @Test void invalidOrOverlappingSectorsAndUnmappedSlotsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout.Sector(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout.Sector(0, 361, 0));
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout.Sector(Double.NaN, 30, 0));
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout(44, 100,
                List.of(new RadialLayout.Sector(-30, 60, 0), new RadialLayout.Sector(0, 45, 1))));
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout(44, 100,
                List.of(new RadialLayout.Sector(0, 45, 0), new RadialLayout.Sector(90, 45, 0))));
        assertThrows(IllegalArgumentException.class, () -> layout().validateSlots(2));
        assertThrows(IllegalArgumentException.class, () -> layout().validateSlots(4));
    }
    @Test void unavailableFamiliesLeaveFixedButtonsInTheirOriginalDirections() {
        // The library indices stay dense even when whole vehicle action families disappear.
        var full = new RadialLayout(44, 100, List.of(new RadialLayout.Sector(-30, 55, 0),
                new RadialLayout.Sector(25, 45, 1), new RadialLayout.Sector(205, 55, 2),
                new RadialLayout.Sector(295, 35, 3)));
        var sparse = new RadialLayout(44, 100, List.of(new RadialLayout.Sector(205, 55, 0),
                new RadialLayout.Sector(295, 35, 1)));
        sparse.validateSlots(2);
        assertEquals(full.slotX(2, 4), sparse.slotX(0, 2));
        assertEquals(full.slotY(3, 4), sparse.slotY(1, 2));
        assertEquals(-1, sparse.hitIndex(0, -80, 2));
        assertEquals(0, sparse.hitIndex(sparse.slotX(0, 2), sparse.slotY(0, 2), 2));
    }
}
