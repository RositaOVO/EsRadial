package org.esradial.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ItemIcon implements IRadialIcon {
    private final ItemStack stack;
    private final float scale;
    public ItemIcon(ItemStack stack, float scale) { this.stack = stack.copy(); this.scale = scale; }
    public void render(GuiGraphics graphics, int x, int y, float animation, float alpha) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale * animation, scale * animation, 1);
            graphics.renderItem(stack, -8, -8);
        } finally { graphics.pose().popPose(); }
    }
}
