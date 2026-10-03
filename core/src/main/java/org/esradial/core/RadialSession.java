package org.esradial.core;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** UI-independent session. All methods are called by one owning thread. */
public final class RadialSession<T> {
    public record Slot<T>(String id, T value, boolean enabled, boolean closeAfterAction,
                          int repeatTicks, Runnable action) {
        public Slot {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Empty slot id");
            Objects.requireNonNull(value); Objects.requireNonNull(action);
            if (repeatTicks < 0) throw new IllegalArgumentException("Negative repeat interval");
        }
    }
    public record Page<T>(String id, RadialLayout layout, List<Slot<T>> slots) {
        public Page {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Empty page id");
            Objects.requireNonNull(layout); slots = List.copyOf(slots);
            if (slots.size() > 64) throw new IllegalArgumentException("Maximum 64 slots per page");
            HashSet<String> ids = new HashSet<>();
            for (Slot<T> slot : slots) if (!ids.add(slot.id()))
                throw new IllegalArgumentException("Duplicate slot id: " + slot.id());
        }
    }
    public enum CloseReason { ACTION, RELEASE, CANCEL, REPLACED, UNAVAILABLE, ERROR }

    private final ArrayDeque<Page<T>> history = new ArrayDeque<>();
    private final Consumer<CloseReason> onClose;
    private Page<T> page;
    private String hoveredId;
    private boolean closed, primaryDown, primaryArmed, releaseConsumed, executing;
    private int repeatProgress;
    private long revision;

    public RadialSession(Page<T> page, Consumer<CloseReason> onClose) {
        this.page = Objects.requireNonNull(page); this.onClose = Objects.requireNonNull(onClose);
    }
    public Page<T> page() { return page; }
    public boolean isClosed() { return closed; }
    public long revision() { return revision; }
    public int hoveredIndex() {
        for (int i = 0; i < page.slots().size(); i++)
            if (page.slots().get(i).id().equals(hoveredId)) return i;
        return -1;
    }
    public Slot<T> hovered() { int i = hoveredIndex(); return i < 0 ? null : page.slots().get(i); }
    public void hover(double dx, double dy) {
        int index = page.layout().hitIndex(dx, dy, page.slots().size());
        String next = index < 0 ? null : page.slots().get(index).id();
        if (!Objects.equals(next, hoveredId)) { hoveredId = next; repeatProgress = 0; }
    }
    /** Update data without resetting the selected stable ID or navigation history. */
    public void replace(Page<T> next) {
        if (closed) return;
        page = Objects.requireNonNull(next); revision++; repeatProgress = 0;
        if (hoveredIndex() < 0) hoveredId = null;
    }
    public void push(Page<T> next) {
        if (closed) return;
        history.push(page); replace(next); hoveredId = null;
    }
    public boolean back() {
        if (closed || history.isEmpty()) return false;
        replace(history.pop()); hoveredId = null; return true;
    }
    public void updatePrimary(boolean down) {
        if (closed) return;
        boolean pressed = down && !primaryDown; primaryDown = down;
        if (!down) { primaryArmed = false; repeatProgress = 0; return; }
        Slot<T> slot = hovered();
        if (pressed) {
            primaryArmed = true;
            confirm(false);
        } else if (primaryArmed && slot != null && slot.enabled() && slot.repeatTicks() > 0
                && ++repeatProgress >= slot.repeatTicks()) {
            repeatProgress = 0; confirm(false);
        }
    }
    public double repeatProgress() {
        Slot<T> slot = hovered();
        return slot == null || slot.repeatTicks() < 1 ? 0 : repeatProgress / (double) slot.repeatTicks();
    }
    public boolean confirmRelease() {
        if (closed) return false;
        boolean success = !releaseConsumed && confirm(true);
        if (!closed) close(CloseReason.RELEASE);
        return success;
    }
    /** Seed physical state on open without firing a click from an already held button. */
    public void seedPrimary(boolean down) { primaryDown = down; primaryArmed = false; }
    public boolean confirm(boolean fromRelease) {
        Slot<T> slot = hovered();
        if (closed || executing || slot == null || !slot.enabled()) return false;
        if (fromRelease && slot.repeatTicks() > 0) return false;
        long before = revision;
        executing = true;
        try { slot.action().run(); }
        catch (RuntimeException error) { close(CloseReason.ERROR); throw error; }
        finally { executing = false; }
        // A navigation callback may replace the page or close this session itself.
        if (!closed && revision == before) {
            releaseConsumed = true;
            if (slot.closeAfterAction()) close(CloseReason.ACTION);
        }
        return true;
    }
    public void close(CloseReason reason) {
        if (closed) return;
        closed = true; hoveredId = null; history.clear(); repeatProgress = 0;
        onClose.accept(reason);
    }
}
