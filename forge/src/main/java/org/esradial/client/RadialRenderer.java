package org.esradial.client;

import net.minecraft.client.gui.GuiGraphics;
import org.esradial.core.RadialSession;

/** Replaceable view. Coordinates and hit testing must use the same logical viewport. */
public interface RadialRenderer {
    boolean open(RadialMenuData menu);
    void update(RadialMenuData menu, RadialSession<RadialMenuData.Visual> session, double animation);
    double mouseX();
    double mouseY();
    default void setNavigation(String breadcrumb, boolean canBack) { }
    default boolean isBackButtonHovered(RadialMenuData menu, boolean canBack) { return false; }
    void render(GuiGraphics graphics);
    void close();
}
