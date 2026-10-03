package org.esradial.core;

/** Clockwise slots, with slot zero centered at twelve o'clock. */
public record RadialLayout(double innerRadius, double outerRadius) {
    public RadialLayout {
        if (!Double.isFinite(innerRadius) || !Double.isFinite(outerRadius)
                || innerRadius < 0 || outerRadius <= innerRadius || outerRadius > 512) {
            throw new IllegalArgumentException("Invalid radial radii");
        }
    }

    public int hitIndex(double dx, double dy, int count) {
        if (count < 1 || !Double.isFinite(dx) || !Double.isFinite(dy)) return -1;
        double radius = Math.hypot(dx, dy);
        // Direction selection remains available beyond the visible ring.
        if (radius < innerRadius) return -1;
        double angle = Math.atan2(dx, -dy);
        double step = Math.PI * 2 / count;
        return Math.floorMod((int) Math.floor((angle + step / 2) / step), count);
    }

    public double slotX(int index, int count) { return Math.sin(angle(index, count)) * iconRadius(); }
    public double slotY(int index, int count) { return -Math.cos(angle(index, count)) * iconRadius(); }
    public double iconRadius() { return (innerRadius + outerRadius) / 2; }
    private double angle(int index, int count) { return count > 0 ? index * Math.PI * 2 / count : 0; }
}
