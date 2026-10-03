package org.esradial.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/** Clockwise angles measured in degrees from twelve o'clock. */
public record RadialLayout(double innerRadius, double outerRadius, List<RadialLayout.Sector> sectors) {
    /** A slot index of -1 reserves an inert empty sector. End boundaries are exclusive. */
    public record Sector(double startDegrees, double sweepDegrees, int slotIndex) {
        public Sector {
            if (!Double.isFinite(startDegrees) || !Double.isFinite(sweepDegrees)
                    || sweepDegrees <= 0 || sweepDegrees > 360 || slotIndex < -1 || slotIndex >= 64)
                throw new IllegalArgumentException("Invalid radial sector");
            startDegrees = normalize(startDegrees);
        }
        public double startRadians() { return Math.toRadians(startDegrees) - Math.PI / 2; }
        public double endRadians() { return startRadians() + Math.toRadians(sweepDegrees); }
        public double centerRadians() { return Math.toRadians(startDegrees + sweepDegrees / 2); }
        public boolean contains(double degrees) { return normalize(degrees - startDegrees) < sweepDegrees; }
    }
    public RadialLayout(double innerRadius, double outerRadius) { this(innerRadius, outerRadius, List.of()); }
    /** A sparse preset for arbitrary catalogs: unequal action sectors and two inert gaps. */
    public static RadialLayout squad(double innerRadius, double outerRadius, int count) {
        if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid slot count");
        if (count == 0) return new RadialLayout(innerRadius, outerRadius);
        double[] weights = {55, 45, 60, 35, 55, 35};
        double total = 75;
        for (int i = 0; i < count; i++) total += weights[i % weights.length];
        double unit = 360 / total, angle = -weights[0] * unit / 2;
        int firstGap = (count - 1) / 2, secondGap = Math.max(firstGap + 1, count - 2);
        List<Sector> areas = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double sweep = weights[i % weights.length] * unit;
            areas.add(new Sector(angle, sweep, i)); angle += sweep;
            if (i == firstGap) { areas.add(new Sector(angle, 40 * unit, -1)); angle += 40 * unit; }
            if (i == secondGap) { areas.add(new Sector(angle, 35 * unit, -1)); angle += 35 * unit; }
        }
        // One/two-action menus may reach the second gap after the last action.
        if (secondGap >= count) areas.add(new Sector(angle, 35 * unit, -1));
        return new RadialLayout(innerRadius, outerRadius, areas);
    }
    public RadialLayout {
        if (!Double.isFinite(innerRadius) || !Double.isFinite(outerRadius)
                || innerRadius < 0 || outerRadius <= innerRadius || outerRadius > 512) {
            throw new IllegalArgumentException("Invalid radial radii");
        }
        sectors = List.copyOf(sectors);
        if (sectors.size() > 128) throw new IllegalArgumentException("Maximum 128 sectors");
        if (!sectors.isEmpty()) sectors = complete(sectors);
    }
    private static List<Sector> complete(List<Sector> source) {
        List<double[]> intervals = new ArrayList<>();
        HashSet<Integer> slots = new HashSet<>();
        for (Sector sector : source) {
            if (sector.slotIndex >= 0 && !slots.add(sector.slotIndex))
                throw new IllegalArgumentException("Duplicate sector slot index");
            double end = sector.startDegrees + sector.sweepDegrees;
            intervals.add(new double[]{sector.startDegrees, Math.min(360, end)});
            if (end > 360) intervals.add(new double[]{0, end - 360});
        }
        intervals.sort(Comparator.comparingDouble(interval -> interval[0]));
        List<Sector> result = new ArrayList<>(source);
        double end = 0;
        for (double[] interval : intervals) {
            if (interval[0] < end - 1e-9) throw new IllegalArgumentException("Overlapping radial sectors");
            if (interval[0] > end + 1e-9) result.add(new Sector(end, interval[0] - end, -1));
            end = interval[1];
        }
        if (end < 360 - 1e-9) result.add(new Sector(end, 360 - end, -1));
        return List.copyOf(result);
    }
    public void validateSlots(int count) {
        if (count < 0 || count > 64) throw new IllegalArgumentException("Invalid slot count");
        if (sectors.isEmpty()) return;
        int assigned = 0;
        for (Sector sector : sectors) if (sector.slotIndex >= 0) {
            if (sector.slotIndex >= count) throw new IllegalArgumentException("Sector refers to missing slot");
            assigned++;
        }
        if (assigned != count) throw new IllegalArgumentException("Every slot needs one sector");
    }
    public List<Sector> sectors(int count) {
        if (!sectors.isEmpty()) return sectors;
        if (count < 1) return List.of(new Sector(0, 360, -1));
        List<Sector> equal = new ArrayList<>(count);
        double step = 360.0 / count;
        for (int i = 0; i < count; i++) equal.add(new Sector(i * step - step / 2, step, i));
        return List.copyOf(equal);
    }
    public Sector sectorForSlot(int index, int count) {
        if (index < 0 || index >= count) throw new IllegalArgumentException("Invalid slot index");
        if (sectors.isEmpty()) return new Sector((index - .5) * 360 / count, 360.0 / count, index);
        return sectors.stream().filter(sector -> sector.slotIndex == index).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Slot has no sector"));
    }

    public int hitIndex(double dx, double dy, int count) {
        if (count < 1 || !Double.isFinite(dx) || !Double.isFinite(dy)) return -1;
        double radius = Math.hypot(dx, dy);
        // Direction selection remains available beyond the visible ring.
        if (radius < innerRadius) return -1;
        double angle = normalize(Math.toDegrees(Math.atan2(dx, -dy)));
        if (sectors.isEmpty()) {
            double step = 360.0 / count;
            return Math.floorMod((int) Math.floor((angle + step / 2) / step), count);
        }
        for (Sector sector : sectors) if (sector.contains(angle)) return sector.slotIndex;
        return -1;
    }

    public double slotX(int index, int count) { return Math.sin(sectorForSlot(index, count).centerRadians()) * iconRadius(); }
    public double slotY(int index, int count) { return -Math.cos(sectorForSlot(index, count).centerRadians()) * iconRadius(); }
    public double iconRadius() { return (innerRadius + outerRadius) / 2; }
    private static double normalize(double degrees) { return (degrees % 360 + 360) % 360; }
}
