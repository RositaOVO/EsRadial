package org.esradial.core;

/** Reserve room for the top navigation and bottom description at automatic GUI scales. */
public final class RadialViewSizing {
    private RadialViewSizing() { }
    /** Normal wheels put their description in the center and need no footer reservation. */
    public static double compactScale(double outerRadius, double width, double height) {
        return Math.min(1,Math.min(Math.max(1,width-32)/(outerRadius*2),Math.max(1,height-64)/(outerRadius*2)));
    }
    public static double scale(double outerRadius, double width, double height) {
        return Math.min(1,Math.min(Math.max(1,width-32)/(outerRadius*2),Math.max(1,height-112)/(outerRadius*2)));
    }
}
