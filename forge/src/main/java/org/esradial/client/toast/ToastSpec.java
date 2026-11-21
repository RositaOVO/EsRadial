package org.esradial.client.toast;

import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Appearance and timing of one toast. Sizes are in GUI-scaled pixels.
 *
 * <p>Use {@link #info(Component, Component)} / {@link #error(Component, Component)} for the default
 * look (dark solid panel, coloured stripe on the left, docked right-centre, slides in from the
 * right edge), then adjust with the {@code with*} methods.</p>
 */
public record ToastSpec(
        Component title,
        Component body,
        int accentArgb,
        int backgroundArgb,
        int bodyArgb,
        int width,
        int height,
        int stripeWidth,
        int margin,
        int offscreenPadding,
        int holdTicks,
        int slideMillis,
        boolean pauseOnHover) {

    public static final int INFO_ACCENT = 0xFFE0B85A;
    public static final int ERROR_ACCENT = 0xFFE05A5A;
    public static final int DEFAULT_BACKGROUND = 0xEE111416;
    public static final int DEFAULT_BODY = 0xFFFFFFFF;

    public ToastSpec {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Toast size must be positive");
        if (stripeWidth < 0 || margin < 0 || offscreenPadding < 0)
            throw new IllegalArgumentException("Negative toast spacing");
        if (holdTicks < 0 || slideMillis < 0) throw new IllegalArgumentException("Negative toast duration");
    }

    public static ToastSpec info(Component title, Component body) {
        return defaults(title, body, INFO_ACCENT);
    }

    public static ToastSpec error(Component title, Component body) {
        return defaults(title, body, ERROR_ACCENT);
    }

    private static ToastSpec defaults(Component title, Component body, int accent) {
        return new ToastSpec(title, body, accent, DEFAULT_BACKGROUND, DEFAULT_BODY,
                248, 88, 3, 8, 48, 50, 220, true);
    }

    public ToastSpec withSize(int newWidth, int newHeight) {
        return new ToastSpec(title, body, accentArgb, backgroundArgb, bodyArgb, newWidth, newHeight,
                stripeWidth, margin, offscreenPadding, holdTicks, slideMillis, pauseOnHover);
    }

    public ToastSpec withHoldTicks(int ticks) {
        return new ToastSpec(title, body, accentArgb, backgroundArgb, bodyArgb, width, height,
                stripeWidth, margin, offscreenPadding, ticks, slideMillis, pauseOnHover);
    }

    public ToastSpec withAccent(int argb) {
        return new ToastSpec(title, body, argb, backgroundArgb, bodyArgb, width, height,
                stripeWidth, margin, offscreenPadding, holdTicks, slideMillis, pauseOnHover);
    }

    public ToastSpec withBackground(int argb) {
        return new ToastSpec(title, body, accentArgb, argb, bodyArgb, width, height,
                stripeWidth, margin, offscreenPadding, holdTicks, slideMillis, pauseOnHover);
    }

    public ToastSpec withSlideMillis(int millis) {
        return new ToastSpec(title, body, accentArgb, backgroundArgb, bodyArgb, width, height,
                stripeWidth, margin, offscreenPadding, holdTicks, millis, pauseOnHover);
    }

    public ToastSpec withPauseOnHover(boolean pause) {
        return new ToastSpec(title, body, accentArgb, backgroundArgb, bodyArgb, width, height,
                stripeWidth, margin, offscreenPadding, holdTicks, slideMillis, pause);
    }
}
