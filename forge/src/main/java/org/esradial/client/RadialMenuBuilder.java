package org.esradial.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.esradial.core.RadialLayout;
import org.esradial.core.RadialSession;
import java.util.ArrayList;
import java.util.List;

public final class RadialMenuBuilder {
    private final ResourceLocation id;
    private Component title = Component.empty();
    private RadialLayout layout = new RadialLayout(44, 96);
    private List<String> colors = List.of("#B824292B", "#C832383A");
    private float speed = 1.25f;
    private final List<RadialSession.Slot<RadialMenuData.Visual>> slots = new ArrayList<>();
    public RadialMenuBuilder(ResourceLocation id) { this.id = id; }
    public RadialMenuBuilder title(Component title) { this.title = title; return this; }
    public RadialMenuBuilder radii(double inner, double outer) { layout = new RadialLayout(inner, outer); return this; }
    public RadialMenuBuilder animationSpeed(float speed) { this.speed = speed; return this; }
    public RadialMenuBuilder ringColors(List<String> colors) { this.colors = List.copyOf(colors); return this; }
    public RadialMenuBuilder slot(String id, ResourceLocation icon, Runnable action, Component label, String color) {
        return slot(id, icon, action, label, color, true);
    }
    public RadialMenuBuilder slot(String id, ResourceLocation icon, Runnable action, Component label, String color, boolean close) {
        return add(id, icon, ItemStack.EMPTY, null, action, label, color, color, close);
    }
    public RadialMenuBuilder slot(String id, IRadialIcon icon, Runnable action, Component label, String color) {
        return add(id, null, ItemStack.EMPTY, icon, action, label, color, color, true);
    }
    public RadialMenuBuilder persistentSlot(String id, ResourceLocation icon, Runnable action, Component label, String color) {
        return persistentSlot(id, icon, action, label, color, color);
    }
    public RadialMenuBuilder persistentSlot(String id, ResourceLocation icon, Runnable action, Component label, String color, String highlight) {
        return add(id, icon, ItemStack.EMPTY, null, action, label, color, highlight, false);
    }
    public RadialMenuBuilder persistentSlot(String id, IRadialIcon icon, Runnable action, Component label, String color) {
        return add(id, null, ItemStack.EMPTY, icon, action, label, color, color, false);
    }
    public RadialMenuBuilder persistentSlot(String id, IRadialIcon icon, Runnable action, Component label, String color, String highlight) {
        return add(id, null, ItemStack.EMPTY, icon, action, label, color, highlight, false);
    }
    public RadialMenuBuilder itemSlot(String id, ItemStack icon, Runnable action, Component label, String color, String highlight, boolean close) {
        return add(id, null, icon, null, action, label, color, highlight, close);
    }
    private RadialMenuBuilder add(String id, ResourceLocation texture, ItemStack item, IRadialIcon nativeIcon,
            Runnable action, Component label, String color, String highlight, boolean close) {
        slots.add(new RadialSession.Slot<>(id, new RadialMenuData.Visual(label, texture, item,
                nativeIcon, color, highlight, Component.empty()), true, close, 0, action)); return this;
    }
    public RadialMenuBuilder disabledLast(Component reason) {
        var slot = last(); var v = slot.value();
        slots.set(slots.size() - 1, new RadialSession.Slot<>(slot.id(), new RadialMenuData.Visual(v.label(),
                v.texture(), v.item(), v.nativeIcon(), v.color(), v.highlight(), reason), false,
                slot.closeAfterAction(), slot.repeatTicks(), slot.action())); return this;
    }
    public RadialMenuBuilder repeatLast(int intervalTicks) {
        if (intervalTicks < 1) throw new IllegalArgumentException("Repeat interval must be positive");
        var slot = last();
        slots.set(slots.size() - 1, new RadialSession.Slot<>(slot.id(), slot.value(), slot.enabled(), false,
                intervalTicks, slot.action())); return this;
    }
    private RadialSession.Slot<RadialMenuData.Visual> last() {
        if (slots.isEmpty()) throw new IllegalStateException("Add a slot first");
        return slots.get(slots.size() - 1);
    }
    public RadialMenuData build() { return new RadialMenuData(id, title, layout, slots, colors, speed); }
}
