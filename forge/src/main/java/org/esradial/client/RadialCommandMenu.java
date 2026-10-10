package org.esradial.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.esradial.core.RadialLayout;
import org.esradial.core.RadialSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Optional mods contribute directories without a hard dependency on each other. */
public final class RadialCommandMenu {
    private record Contribution(Supplier<RadialMenuData> root, BooleanSupplier available, Runnable prepare) { }
    private static final LinkedHashMap<String, Contribution> contributions = new LinkedHashMap<>();
    private static final java.util.Map<String, Integer> order = new java.util.HashMap<>();
    private RadialCommandMenu() { }
    public static void register(String owner, Supplier<RadialMenuData> root,
                                BooleanSupplier available, Runnable prepare) {
        contributions.put(owner, new Contribution(root, available, prepare));
    }
    public static void setOrder(String slotId, int priority) { order.put(slotId, priority); }
    /** Called before opening only; it never mutates an already visible menu. */
    public static RadialMenuData compose(RadialMenuData fallback) {
        var slots = new ArrayList<RadialSession.Slot<RadialMenuData.Visual>>();
        for (var contributor : contributions.values()) {
            if (!contributor.available().getAsBoolean()) continue;
            contributor.prepare().run();
            var root = contributor.root().get();
            if (root != null) slots.addAll(root.slots());
        }
        if (slots.isEmpty()) return fallback;
        slots.sort(java.util.Comparator.comparingInt(s -> order.getOrDefault(s.id(), 1000)));
        return new RadialMenuData(ResourceLocation.fromNamespaceAndPath("esradial", "command_root"),
            Component.literal("指挥菜单"), RadialLayout.squad(44, 96, slots.size()), slots,
            fallback.ringColors(), fallback.animationSpeed());
    }
}
