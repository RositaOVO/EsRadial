package org.esradial.client;

/** Convenience lifecycle facade for existing tick-driven consumers. Rendering is owned by AUI. */
public final class RadialMenuOverlay {
    public static final RadialMenuOverlay INSTANCE = new RadialMenuOverlay();
    private RadialMenuOverlay() { }
    public boolean isActive() { return RadialMenuClientApi.isActive(); }
    public void close() { RadialMenuClientApi.close(); }
    public boolean mouseClicked(double ignoredX, double ignoredY, int button) {
        return button == 0 && RadialMenuClientApi.confirmHovered();
    }
}
