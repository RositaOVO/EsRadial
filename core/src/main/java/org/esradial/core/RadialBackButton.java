package org.esradial.core;

/** Drawing and hit testing share these wheel-relative logical coordinates. */
public record RadialBackButton(double left, double top, double width, double height) {
    public static RadialBackButton above(double outerRadius, double viewportHeight) {
        return new RadialBackButton(-38, Math.max(8 - viewportHeight / 2, -outerRadius - 26), 76, 20);
    }
    /** Upper half of the central disk, with all corners inside its rim. */
    public static RadialBackButton inside(double innerRadius) {
        double width = Math.min(64, innerRadius * 1.4);
        return new RadialBackButton(-width / 2, -innerRadius * .64,
            width, Math.min(18, innerRadius * .48));
    }
    public boolean contains(double x, double y) {
        return Double.isFinite(x) && Double.isFinite(y)
            && x >= left && x < left + width && y >= top && y < top + height;
    }
}
