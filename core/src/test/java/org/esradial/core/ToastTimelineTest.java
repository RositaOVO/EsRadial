package org.esradial.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToastTimelineTest {
    private static final long SLIDE = 200, HOLD = 2_500;

    @Test
    void phasesFollowSlideHoldSlide() {
        ToastTimeline t = new ToastTimeline(SLIDE, HOLD, 1_000);
        assertEquals(ToastTimeline.Phase.ENTERING, t.phase(1_000));
        assertEquals(ToastTimeline.Phase.HOLDING, t.phase(1_000 + SLIDE));
        assertEquals(ToastTimeline.Phase.EXITING, t.phase(1_000 + SLIDE + HOLD));
        assertEquals(ToastTimeline.Phase.DONE, t.phase(1_000 + SLIDE * 2 + HOLD));
        assertTrue(t.isDone(1_000 + SLIDE * 2 + HOLD));
    }

    @Test
    void visibilityAnimatesInAndOut() {
        ToastTimeline t = new ToastTimeline(SLIDE, HOLD, 0);
        assertEquals(0F, t.visibility(0), 1e-6);
        float mid = t.visibility(SLIDE / 2);
        assertTrue(mid > 0.5F && mid < 1F, "ease-out is past halfway at half time: " + mid);
        assertEquals(1F, t.visibility(SLIDE), 1e-6);
        assertEquals(1F, t.visibility(SLIDE + HOLD - 1), 1e-6);
        float out = t.visibility(SLIDE + HOLD + SLIDE / 2);
        assertTrue(out > 0.5F && out < 1F, "ease-in is still mostly visible at half time: " + out);
        assertEquals(0F, t.visibility(SLIDE * 2 + HOLD), 1e-6);
    }

    @Test
    void hoverDuringHoldPausesTimer() {
        ToastTimeline t = new ToastTimeline(SLIDE, HOLD, 0);
        t.setHovered(true, SLIDE + 100);
        assertTrue(t.isPaused());
        long later = SLIDE + HOLD + 5_000;
        assertEquals(ToastTimeline.Phase.HOLDING, t.phase(later));
        t.setHovered(false, later);
        assertFalse(t.isPaused());
        // 100 ms of hold were used before hovering; the rest remains
        assertEquals(ToastTimeline.Phase.HOLDING, t.phase(later + HOLD - 101));
        assertEquals(ToastTimeline.Phase.EXITING, t.phase(later + HOLD - 100));
    }

    @Test
    void hoverDuringEntryStartsPausingOnceHolding() {
        ToastTimeline t = new ToastTimeline(SLIDE, HOLD, 0);
        t.setHovered(true, 50);
        assertFalse(t.isPaused());
        t.setHovered(true, SLIDE);
        assertTrue(t.isPaused());
        assertEquals(1F, t.visibility(SLIDE + 10_000), 1e-6);
    }

    @Test
    void hoverDuringExitDoesNotPause() {
        ToastTimeline t = new ToastTimeline(SLIDE, HOLD, 0);
        t.setHovered(true, SLIDE + HOLD + 10);
        assertFalse(t.isPaused());
        assertTrue(t.isDone(SLIDE * 2 + HOLD));
    }

    @Test
    void zeroSlideIsInstant() {
        ToastTimeline t = new ToastTimeline(0, HOLD, 0);
        assertEquals(1F, t.visibility(0), 1e-6);
        assertEquals(0F, t.visibility(HOLD), 1e-6);
        assertTrue(t.isDone(HOLD));
    }

    @Test
    void rejectsNegativeDurations() {
        assertThrows(IllegalArgumentException.class, () -> new ToastTimeline(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ToastTimeline(0, -1, 0));
    }
}
