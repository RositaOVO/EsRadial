package org.esradial.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.esradial.core.RadialLayout;
import org.esradial.core.RadialSession;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public record RadialMenuData(ResourceLocation id, Component title, RadialLayout layout,
        List<RadialSession.Slot<Visual>> slots, List<String> ringColors, float animationSpeed,
        Supplier<Progress> progress) {
    /** Null slotId means a center action; hovering a named slot moves its progress outside. */
    public record Progress(String slotId, double value, String color) {
        public static final Progress NONE = new Progress(null, 0, "#ffffff");
        public Progress {
            if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("Invalid progress");
            Objects.requireNonNull(color);
        }
    }
    public RadialMenuData(ResourceLocation id, Component title, RadialLayout layout,
            List<RadialSession.Slot<Visual>> slots, List<String> ringColors, float animationSpeed) {
        this(id, title, layout, slots, ringColors, animationSpeed, () -> Progress.NONE);
    }
    public record Visual(Component label, ResourceLocation texture, ItemStack item,
                         IRadialIcon nativeIcon, String color, String highlight, Component denial, int textureTint) {
        public Visual(Component label, ResourceLocation texture, ItemStack item, IRadialIcon nativeIcon,
                String color, String highlight, Component denial) {
            this(label,texture,item,nativeIcon,color,highlight,denial,0xFFFFFFFF);
        }
        public Visual {
            Objects.requireNonNull(label); item = item == null ? ItemStack.EMPTY : item.copy();
            Objects.requireNonNull(color); Objects.requireNonNull(highlight); Objects.requireNonNull(denial);
        }
    }
    public RadialMenuData {
        Objects.requireNonNull(id); Objects.requireNonNull(title); Objects.requireNonNull(layout);
        Objects.requireNonNull(progress);
        slots = List.copyOf(slots); ringColors = List.copyOf(ringColors);
        if (ringColors.isEmpty()) throw new IllegalArgumentException("Empty ring colors");
        if (!Float.isFinite(animationSpeed) || animationSpeed <= 0) throw new IllegalArgumentException("Invalid animation speed");
        new RadialSession.Page<>(id.toString(), layout, slots);
    }
    public RadialSession.Page<Visual> page() { return new RadialSession.Page<>(id.toString(), layout, slots); }
}
