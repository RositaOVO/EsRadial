package org.esradial.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Geometry-only editor: empty sectors are movable pieces, never action indices. */
public final class RadialLayoutEditor {
    public enum Snap { BOTH, THIRTY, FORTY_FIVE, FREE }
    private record Piece(int slotIndex, double sweep) { }
    private final double inner, outer;
    private final List<Piece> pieces = new ArrayList<>();
    private double origin;

    public RadialLayoutEditor(RadialLayout layout, int slotCount) {
        layout.validateSlots(slotCount);
        inner = layout.innerRadius(); outer = layout.outerRadius();
        var sectors = layout.sectors(slotCount).stream()
            .sorted(Comparator.comparingDouble(RadialLayout.Sector::startDegrees)).toList();
        origin = sectors.get(0).startDegrees();
        for (var sector : sectors) pieces.add(new Piece(sector.slotIndex(), sector.sweepDegrees()));
    }
    public RadialLayout layout() {
        List<RadialLayout.Sector> sectors = new ArrayList<>();
        double start = origin;
        for (var piece : pieces) {
            sectors.add(new RadialLayout.Sector(start, piece.sweep(), piece.slotIndex()));
            start += piece.sweep();
        }
        return new RadialLayout(inner, outer, sectors);
    }
    public int pieceAt(double x, double y) {
        if (Math.hypot(x, y) < inner || Math.hypot(x, y) > outer + 12) return -1;
        var sectors = layout().sectors();
        double angle = angle(x, y);
        for (int i = 0; i < sectors.size(); i++) if (sectors.get(i).contains(angle)) return i;
        return -1;
    }
    public int boundaryAt(double x, double y) {
        double radius = Math.hypot(x, y);
        if (radius < inner - 4 || radius > outer + 8 || pieces.size() < 2) return -1;
        var sectors = layout().sectors();
        for (int i = 0; i < sectors.size(); i++) {
            double difference = delta(angle(x, y) - sectors.get(i).startDegrees());
            if (Math.abs(Math.toRadians(difference) * radius) <= 4) return i;
        }
        return -1;
    }
    /** Insert a dragged action or gap at the target piece's location, preserving all widths. */
    public void move(int from, int to) {
        if (from < 0 || from >= pieces.size() || to < 0 || to >= pieces.size() || from == to) return;
        Piece piece = pieces.remove(from); pieces.add(to, piece);
    }
    /** Drag the boundary preceding piece i; its two neighbours retain their combined width. */
    public void resizeBoundary(int i, double requestedAngle) {
        if (!Double.isFinite(requestedAngle) || i < 0 || i >= pieces.size() || pieces.size() < 2) return;
        int previous = Math.floorMod(i - 1, pieces.size());
        double shift = delta(requestedAngle - layout().sectors().get(i).startDegrees());
        Piece left = pieces.get(previous), right = pieces.get(i);
        double minimum = Math.min(4, Math.min(left.sweep(), right.sweep()));
        shift = Math.max(minimum - left.sweep(), Math.min(right.sweep() - minimum, shift));
        pieces.set(previous, new Piece(left.slotIndex(), left.sweep() + shift));
        pieces.set(i, new Piece(right.slotIndex(), right.sweep() - shift));
        if (i == 0) origin = normalize(origin + shift);
    }
    public void rotate(double degrees) { if (Double.isFinite(degrees)) origin = normalize(origin + degrees); }
    public static double snapAngle(double angle, Snap snap) {
        if (!Double.isFinite(angle)) throw new IllegalArgumentException("Non-finite angle");
        if (snap == Snap.FREE) return normalize(angle);
        double thirty = Math.round(angle / 30) * 30;
        double fortyFive = Math.round(angle / 45) * 45;
        return normalize(snap == Snap.THIRTY ? thirty : snap == Snap.FORTY_FIVE ? fortyFive
            : Math.abs(angle - thirty) <= Math.abs(angle - fortyFive) ? thirty : fortyFive);
    }
    /** Pick a legal snapped boundary, rather than clamping a snapped angle off its grid. */
    public void resizeBoundary(int i, double angle, Snap snap) {
        if (snap == Snap.FREE) { resizeBoundary(i, angle); return; }
        if (!Double.isFinite(angle) || i < 0 || i >= pieces.size() || pieces.size() < 2) return;
        double current = layout().sectors().get(i).startDegrees();
        Piece left = pieces.get(Math.floorMod(i - 1, pieces.size())), right = pieces.get(i);
        double minimum = Math.min(4, Math.min(left.sweep(), right.sweep()));
        double best = Double.NaN, distance = Double.POSITIVE_INFINITY;
        for (int candidate = 0; candidate < 360; candidate += 15) {
            if (!onGrid(candidate, snap)) continue;
            double shift = delta(candidate - current);
            if (shift < minimum - left.sweep() - 1e-8 || shift > right.sweep() - minimum + 1e-8) continue;
            double nextDistance = Math.abs(delta(candidate - angle));
            if (nextDistance < distance) { best = candidate; distance = nextDistance; }
        }
        if (Double.isFinite(best)) resizeBoundary(i, best);
    }
    public void rotateSnapped(int direction, Snap snap) {
        if (direction == 0) return;
        if (snap == Snap.FREE) { rotate(Math.signum(direction) * 5); return; }
        double best = 360;
        for (int candidate = 0; candidate < 360; candidate += 15) {
            if (!onGrid(candidate, snap)) continue;
            double step = normalize((candidate - origin) * Math.signum(direction));
            if (step > 1e-8 && step < best) best = step;
        }
        rotate(best * Math.signum(direction));
    }
    private static boolean onGrid(int angle, Snap snap) {
        return snap == Snap.THIRTY ? angle % 30 == 0 : snap == Snap.FORTY_FIVE ? angle % 45 == 0
            : angle % 30 == 0 || angle % 45 == 0;
    }
    public static double angle(double x, double y) { return normalize(Math.toDegrees(Math.atan2(x, -y))); }
    private static double normalize(double degrees) { return (degrees % 360 + 360) % 360; }
    private static double delta(double degrees) { return normalize(degrees + 180) - 180; }
}
