package org.esradial.client;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RadialMenuRegistry {
    private record Owned(String owner, RadialMenuData menu) { }
    private static final Map<ResourceLocation, Owned> MENUS = new HashMap<>();
    private RadialMenuRegistry() { }
    public static void setMenus(String owner, List<RadialMenuData> menus) {
        if (owner == null || owner.isBlank()) throw new IllegalArgumentException("Empty owner");
        Map<ResourceLocation, Owned> replacement = new HashMap<>();
        for (RadialMenuData menu : menus) {
            Owned old = MENUS.get(menu.id());
            if (old != null && !old.owner().equals(owner)) throw new IllegalArgumentException("Menu belongs to " + old.owner());
            if (replacement.put(menu.id(), new Owned(owner, menu)) != null) throw new IllegalArgumentException("Duplicate menu");
        }
        MENUS.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner));
        MENUS.putAll(replacement);
    }
    public static RadialMenuData getRuntimeMenu(ResourceLocation id) {
        Owned entry = MENUS.get(id); return entry == null ? null : entry.menu();
    }
    public static void clear(String owner) { MENUS.entrySet().removeIf(entry -> entry.getValue().owner().equals(owner)); }
}
