package org.esradial.client;

import net.minecraft.client.gui.GuiGraphics;

/** Optional native icon callback; it must restore any pose/render state it changes. */
@FunctionalInterface
public interface IRadialIcon {
    void render(GuiGraphics graphics, int x, int y, float scale, float alpha);
}
