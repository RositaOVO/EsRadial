package org.esradial.core;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RadialSessionTest {
    private final RadialLayout layout = new RadialLayout(44, 100);
    private final AtomicInteger calls = new AtomicInteger();
    private final List<RadialSession.CloseReason> closed = new ArrayList<>();
    private RadialSession.Slot<String> slot(String id, boolean enabled, boolean close, int interval) {
        return new RadialSession.Slot<>(id, id, enabled, close, interval, calls::incrementAndGet);
    }
    @SafeVarargs private final RadialSession.Page<String> page(String id, RadialSession.Slot<String>... slots) {
        return new RadialSession.Page<>(id, layout, List.of(slots));
    }
    private RadialSession<String> session(RadialSession.Page<String> page) {
        return new RadialSession<>(page, closed::add);
    }
    @Test void directionIsClockwiseFromTopAndNotRestrictedToIconBounds() {
        assertEquals(0, layout.hitIndex(0, -1000, 4));
        assertEquals(1, layout.hitIndex(1000, 0, 4));
        assertEquals(2, layout.hitIndex(0, 1000, 4));
        assertEquals(3, layout.hitIndex(-1000, 0, 4));
        assertEquals(-1, layout.hitIndex(0, 0, 4));
        assertEquals(-1, layout.hitIndex(0, -43.9, 4));
        assertEquals(-1, layout.hitIndex(Double.NaN, 10, 4));
        assertEquals(-1, layout.hitIndex(0, -100, 0));
    }
    @Test void everyDisplayedIconHitsItsOwnSlotAcrossPageSizes() {
        for (int count = 1; count <= 64; count++) for (int i = 0; i < count; i++)
            assertEquals(i, layout.hitIndex(layout.slotX(i, count), layout.slotY(i, count), count));
    }
    @Test void invalidRadiiAndDuplicateIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout(100, 44));
        assertThrows(IllegalArgumentException.class, () -> new RadialLayout(Double.NaN, 100));
        assertThrows(IllegalArgumentException.class, () -> page("a", slot("x", true, true, 0), slot("x", true, true, 0)));
    }
    @Test void cancelOnKeyReleaseNeverRunsHighlightedAction() {
        var s = session(page("root", slot("a", true, true, 0)));
        s.hover(0, -80); s.close(RadialSession.CloseReason.RELEASE);
        assertEquals(0, calls.get()); assertEquals(List.of(RadialSession.CloseReason.RELEASE), closed);
    }
    @Test void quickPressReleaseIsCapturedWithoutWaitingForATick() {
        var s = session(page("root", slot("a", true, true, 0)));
        s.hover(0, -80); s.updatePrimary(true); s.updatePrimary(false);
        assertEquals(1, calls.get()); assertEquals(List.of(RadialSession.CloseReason.ACTION), closed);
    }
    @Test void holdingOrdinaryOptionDoesNotRunEveryTick() {
        var s = session(page("root", slot("a", true, false, 0)));
        s.hover(0, -80);
        for (int i = 0; i < 60; i++) s.updatePrimary(true);
        assertEquals(1, calls.get());
        s.updatePrimary(false); s.updatePrimary(true); assertEquals(2, calls.get());
    }
    @Test void disabledOptionCannotRunOnClickRepeatOrOptionalReleaseMode() {
        var s = session(page("root", slot("a", false, false, 3)));
        s.hover(0, -80); for (int i = 0; i < 20; i++) s.updatePrimary(true);
        assertFalse(s.confirmRelease()); assertEquals(0, calls.get()); assertTrue(s.isClosed());
    }
    @Test void repeatRunsAtIntervalAndStopsAsSoonAsMouseIsReleased() {
        var s = session(page("root", slot("a", true, false, 3)));
        s.hover(0, -80); s.updatePrimary(true); assertEquals(1, calls.get());
        s.updatePrimary(true); assertEquals(1.0 / 3, s.repeatProgress(), 0.0001);
        s.updatePrimary(true); assertEquals(1, calls.get());
        s.updatePrimary(true); assertEquals(2, calls.get());
        s.updatePrimary(false); assertEquals(0, s.repeatProgress());
        for (int i = 0; i < 20; i++) s.updatePrimary(false);
        assertEquals(2, calls.get()); assertFalse(s.isClosed());
    }
    @Test void centerOrDisabledSectorStopsRepeat() {
        var s = session(page("root", slot("a", true, false, 3), slot("b", false, false, 3)));
        s.hover(0, -80); s.updatePrimary(true); s.updatePrimary(true);
        s.hover(0, 0); for (int i = 0; i < 10; i++) s.updatePrimary(true);
        assertEquals(1, calls.get()); assertEquals(0, s.repeatProgress());
        s.hover(0, 80); for (int i = 0; i < 10; i++) s.updatePrimary(true);
        assertEquals(1, calls.get());
    }
    @Test void openingWhileMouseAlreadyHeldDoesNotExecuteOrRepeat() {
        var s = session(page("root", slot("a", true, false, 1)));
        s.seedPrimary(true); s.hover(0, -80);
        for (int i = 0; i < 20; i++) s.updatePrimary(true);
        assertEquals(0, calls.get());
        s.updatePrimary(false); s.updatePrimary(true); assertEquals(1, calls.get());
    }
    @Test void clickNavigationKeepsWheelOpenAndDoesNotClickChildWhileHeld() {
        @SuppressWarnings("unchecked") RadialSession<String>[] holder = new RadialSession[1];
        var child = page("child", slot("leaf", true, true, 0));
        var nav = new RadialSession.Slot<>("nav", "nav", true, true, 0, () -> holder[0].push(child));
        var s = holder[0] = session(page("root", nav));
        s.hover(0, -80); s.updatePrimary(true); assertEquals("child", s.page().id());
        assertFalse(s.isClosed()); assertEquals(-1, s.hoveredIndex());
        s.hover(0, -80); s.updatePrimary(true); assertEquals(0, calls.get());
        assertTrue(s.back()); assertEquals("root", s.page().id()); assertFalse(s.back());
    }
    @Test void liveReplacementRetainsSelectionByIdAcrossReordering() {
        var a = slot("a", true, false, 0); var b = slot("b", true, false, 0);
        var s = session(page("root", a, b)); s.hover(0, -80);
        s.replace(page("root", b, a)); assertEquals(1, s.hoveredIndex());
        s.replace(page("root", b)); assertEquals(-1, s.hoveredIndex());
    }
    @Test void optionalReleaseModeDoesNotDoubleExecutePersistentClickAndStillCloses() {
        var s = session(page("root", slot("a", true, false, 0)));
        s.hover(0, -80); s.updatePrimary(true);
        assertFalse(s.confirmRelease()); assertEquals(1, calls.get()); assertTrue(s.isClosed());
    }
    @Test void failingActionClosesExactlyOnceAndRejectsFurtherInput() {
        var broken = new RadialSession.Slot<>("broken", "broken", true, true, 0,
            () -> { throw new IllegalStateException("test"); });
        var s = session(page("root", broken)); s.hover(0, -80);
        assertThrows(IllegalStateException.class, () -> s.updatePrimary(true));
        s.close(RadialSession.CloseReason.CANCEL); s.updatePrimary(true);
        assertEquals(List.of(RadialSession.CloseReason.ERROR), closed);
    }
}
