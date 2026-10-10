package org.esradial.core;

/** Give each on-screen pixel a backing pixel without changing logical hit coordinates. */
public final class RadialCanvasResolution {
    private RadialCanvasResolution() { }
    public static int scale(int logicalExtent, double documentToGui, int framebufferWidth, int guiWidth) {
        if (logicalExtent <= 0 || !Double.isFinite(documentToGui) || documentToGui <= 0
                || framebufferWidth <= 0 || guiWidth <= 0) return 1;
        double density = documentToGui * framebufferWidth / guiWidth;
        int limit = Math.max(1, 2048 / logicalExtent);
        return (int) Math.max(1, Math.min(limit, Math.ceil(density)));
    }
}
