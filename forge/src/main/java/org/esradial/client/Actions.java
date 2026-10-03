package org.esradial.client;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Optional named callbacks for adapters. There is no script evaluation or network dispatch. */
public final class Actions {
    private static final Map<ResourceLocation, Consumer<Params>> HANDLERS = new HashMap<>();
    private Actions() { }
    public record Params(Map<String, ?> values) {
        public String getString(String key, String fallback) {
            Object value = values.get(key); return value == null ? fallback : value.toString();
        }
    }
    public static void register(ResourceLocation id, Consumer<Params> handler) {
        if (HANDLERS.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(handler)) != null)
            throw new IllegalArgumentException("Action already registered: " + id);
    }
    public static Runnable script(ResourceLocation id, Map<String, ?> values) {
        Params params = new Params(Map.copyOf(values));
        return () -> {
            Consumer<Params> handler = HANDLERS.get(id);
            if (handler == null) throw new IllegalStateException("Unregistered action: " + id);
            handler.accept(params);
        };
    }
}
