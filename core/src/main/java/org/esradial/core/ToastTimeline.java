package org.esradial.core;

/**
 * UI-independent lifetime of a single toast: slide in, hold, slide out.
 *
 * <p>Time is supplied by the caller in milliseconds, so the class is deterministic and testable.
 * Call {@link #setHovered(boolean, long)} every frame: while hovered during the hold phase the
 * timer is paused, so the toast stays until the pointer leaves. Hovering during the entry
 * animation takes effect once the toast is fully shown; hovering during slide-out has no effect.</p>
 */
public final class ToastTimeline {
    public enum Phase { ENTERING, HOLDING, EXITING, DONE }

    private final long slideMillis;
    private final long holdMillis;
    private final long startedAt;
    private long pausedTotal;
    private long pauseStartedAt = -1L;

    /**
     * @param slideMillis duration of each of the slide-in and slide-out animations (&gt;= 0)
     * @param holdMillis  time fully visible between the two animations (&gt;= 0)
     * @param now         creation time in milliseconds
     */
    public ToastTimeline(long slideMillis, long holdMillis, long now) {
        if (slideMillis < 0 || holdMillis < 0) throw new IllegalArgumentException("Negative duration");
        this.slideMillis = slideMillis;
        this.holdMillis = holdMillis;
        this.startedAt = now;
    }

    public void setHovered(boolean hovered, long now) {
        if (hovered) {
            if (pauseStartedAt < 0 && phase(now) == Phase.HOLDING) pauseStartedAt = now;
        } else if (pauseStartedAt >= 0) {
            pausedTotal += Math.max(0L, now - pauseStartedAt);
            pauseStartedAt = -1L;
        }
    }

    public boolean isPaused() {
        return pauseStartedAt >= 0;
    }

    public Phase phase(long now) {
        long t = effectiveElapsed(now);
        if (t < slideMillis) return Phase.ENTERING;
        if (t < slideMillis + holdMillis) return Phase.HOLDING;
        if (t < slideMillis * 2 + holdMillis) return Phase.EXITING;
        return Phase.DONE;
    }

    public boolean isDone(long now) {
        return phase(now) == Phase.DONE;
    }

    /**
     * How far the toast is shown: 0 = fully off-screen, 1 = at its resting position.
     * Cubic ease-out on entry, cubic ease-in on exit.
     */
    public float visibility(long now) {
        long t = effectiveElapsed(now);
        if (t < slideMillis) return easeOut(t / (float) slideMillis);
        t -= slideMillis;
        if (t < holdMillis) return 1F;
        t -= holdMillis;
        if (t < slideMillis) return 1F - easeIn(t / (float) slideMillis);
        return 0F;
    }

    private long effectiveElapsed(long now) {
        long paused = pausedTotal;
        if (pauseStartedAt >= 0) paused += Math.max(0L, now - pauseStartedAt);
        return Math.max(0L, now - startedAt - paused);
    }

    private static float easeOut(float x) {
        float inv = 1F - clamp(x);
        return 1F - inv * inv * inv;
    }

    private static float easeIn(float x) {
        float c = clamp(x);
        return c * c * c;
    }

    private static float clamp(float x) {
        return x < 0F ? 0F : Math.min(1F, x);
    }
}
