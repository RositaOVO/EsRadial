package org.esradial.client;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.esradial.core.RadialLayout;
import org.esradial.core.RadialSession;
import java.io.IOException;
import java.util.List;

/** Automatic customization for every caller, including dynamically replaced pages. */
public final class RadialLayouts {
    private static final RadialLayoutStore STORE = new RadialLayoutStore(FMLPaths.CONFIGDIR.get().resolve("esradial/layouts.json"));
    private RadialLayouts() { }
    static List<String> ids(RadialMenuData menu) { return menu.slots().stream().map(RadialSession.Slot::id).toList(); }
    public static RadialMenuData apply(RadialMenuData menu) {
        try { return withLayout(menu, STORE.load(menu.id().toString(), menu.layout(), ids(menu))); }
        catch (IOException error) {
            LogUtils.getLogger().warn("EsRadial: using default layout for {}: {}", menu.id(), error.getMessage());
            return menu;
        }
    }
    static void save(RadialMenuData menu) throws IOException { STORE.save(menu.id().toString(), menu.layout(), ids(menu)); }
    static RadialMenuData withLayout(RadialMenuData menu, RadialLayout layout) {
        return new RadialMenuData(menu.id(), menu.title(), layout, menu.slots(), menu.ringColors(), menu.animationSpeed(), menu.progress());
    }
}
