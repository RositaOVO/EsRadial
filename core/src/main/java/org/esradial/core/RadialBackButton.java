package org.esradial.core;

/** Drawing and hit testing share these wheel-relative logical coordinates. */
public record RadialBackButton(double left, double top, double width, double height) {
    public static RadialBackButton above(double outerRadius, double viewportHeight) {
        return new RadialBackButton(-38, Math.max(8 - viewportHeight / 2, -outerRadius - 26), 76, 20);
    }
    public boolean contains(double x, double y) {
        return Double.isFinite(x) && Double.isFinite(y)
            && x >= left && x < left + width && y >= top && y < top + height;
    }
}
