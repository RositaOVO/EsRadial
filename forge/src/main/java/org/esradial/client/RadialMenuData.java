package org.esradial.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.esradial.core.RadialLayout;
import org.esradial.core.RadialSession;
import java.util.List;
import java.util.Objects;

public record RadialMenuData(ResourceLocation id, Component title, RadialLayout layout,
        List<RadialSession.Slot<Visual>> slots, List<String> ringColors, float animationSpeed) {
    public record Visual(Component label, ResourceLocation texture, ItemStack item,
                         IRadialIcon nativeIcon, String color, String highlight, Component denial) {
        public Visual {
            Objects.requireNonNull(label); item = item == null ? ItemStack.EMPTY : item.copy();
            Objects.requireNonNull(color); Objects.requireNonNull(highlight); Objects.requireNonNull(denial);
        }
    }
    public RadialMenuData {
        Objects.requireNonNull(id); Objects.requireNonNull(title); Objects.requireNonNull(layout);
        slots = List.copyOf(slots); ringColors = List.copyOf(ringColors);
        if (ringColors.isEmpty()) throw new IllegalArgumentException("Empty ring colors");
        if (!Float.isFinite(animationSpeed) || animationSpeed <= 0) throw new IllegalArgumentException("Invalid animation speed");
        new RadialSession.Page<>(id.toString(), layout, slots);
    }
    public RadialSession.Page<Visual> page() { return new RadialSession.Page<>(id.toString(), layout, slots); }
}
