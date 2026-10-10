package org.esradial.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.esradial.core.RadialSession;
import org.lwjgl.glfw.GLFW;
import java.util.Objects;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** One client-wide wheel, with owner-aware close and interchangeable ApricityUI views. */
public final class RadialMenuClientApi {
    public record OpenOptions(String owner, BooleanSupplier heldKey, boolean confirmOnRelease,
                              Consumer<RadialSession.CloseReason> onClose) {
        public OpenOptions {
            if (owner == null || owner.isBlank()) throw new IllegalArgumentException("Empty owner");
            Objects.requireNonNull(onClose);
            if (confirmOnRelease && heldKey == null) throw new IllegalArgumentException("Release mode needs heldKey");
        }
        public static OpenOptions click(String owner) { return new OpenOptions(owner, null, false, reason -> { }); }
        public static OpenOptions hold(String owner, BooleanSupplier heldKey) { return new OpenOptions(owner, heldKey, false, reason -> { }); }
    }
    private static boolean initialized, closing, restoreMouse;
    private static long animationStart;
    private static double closeFrom = 1;
    private static RadialMenuData menu;
    private static RadialMenuData defaultMenu;
    private static RadialSession<RadialMenuData.Visual> session;
    private static OpenOptions options;
    private static RadialRenderer renderer;
    private static final ArrayDeque<RadialMenuData> history = new ArrayDeque<>();
    private static Supplier<RadialRenderer> rendererFactory = AuiRadialRenderer::new;
    private RadialMenuClientApi() { }

    public static void initialize() {
        if (initialized) return; initialized = true;
        MinecraftForge.EVENT_BUS.addListener(RadialMenuClientApi::tick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, RadialMenuClientApi::onMouse);
        MinecraftForge.EVENT_BUS.addListener(RadialMenuClientApi::onKey);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, RadialMenuClientApi::render);
    }
    public static void setRendererFactory(Supplier<RadialRenderer> factory) {
        requireClientThread();
        if (isActive()) throw new IllegalStateException("Close the current wheel before changing renderer");
        rendererFactory = Objects.requireNonNull(factory);
    }
    public static boolean open(ResourceLocation id) {
        RadialMenuData data = RadialMenuRegistry.getRuntimeMenu(id);
        return data != null && open(data, OpenOptions.click(id.getNamespace()));
    }
    public static boolean open(RadialMenuData data, OpenOptions openOptions) {
        requireClientThread(); initialize();
        Objects.requireNonNull(data); Objects.requireNonNull(openOptions);
        Minecraft mc = Minecraft.getInstance();
        if (isActive() || mc.player == null || mc.level == null || mc.screen != null || !mc.isWindowActive()) return false;
        RadialMenuData original = data;
        data = RadialLayouts.apply(data);
        RadialRenderer view = Objects.requireNonNull(rendererFactory.get());
        try { if (!view.open(data)) { view.close(); return false; } }
        catch (RuntimeException error) { view.close(); LogUtils.getLogger().error("EsRadial template could not open", error); return false; }
        menu = data; defaultMenu = original; options = openOptions; renderer = view; closing = false;
        session = new RadialSession<>(data.page(), RadialMenuClientApi::beginClose);
        history.clear();
        session.seedPrimary(GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
        animationStart = System.nanoTime();
        restoreMouse = mc.mouseHandler.isMouseGrabbed();
        mc.mouseHandler.releaseMouse();
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), mc.getWindow().getScreenWidth() / 2.0, mc.getWindow().getScreenHeight() / 2.0);
        mc.options.keyAttack.setDown(false); mc.options.keyUse.setDown(false);
        return true;
    }
    public static boolean isActive() { return session != null; }
    public static boolean isOwnedBy(String owner) { return options != null && options.owner().equals(owner); }
    public static double repeatProgress() { return session == null || closing ? 0 : session.repeatProgress(); }
    public static Optional<ResourceLocation> activeMenuId() { return menu == null ? Optional.empty() : Optional.of(menu.id()); }
    public static int hoveredSlotIndex() { return session == null || closing ? -1 : session.hoveredIndex(); }
    public static boolean isCenterHovered() {
        return session != null && !closing && Math.hypot(renderer.mouseX(), renderer.mouseY()) < menu.layout().innerRadius();
    }
    public static boolean replace(RadialMenuData data) {
        requireClientThread(); if (session == null || closing) return false;
        defaultMenu = data; menu = RadialLayouts.apply(data); session.replace(menu.page()); return true;
    }
    public static boolean navigate(RadialMenuData data) {
        requireClientThread(); if (session == null || closing) return false;
        history.push(defaultMenu); defaultMenu = data;
        menu = RadialLayouts.apply(data); session.push(menu.page()); return true;
    }
    public static boolean back() {
        requireClientThread(); if (session == null || closing || !session.back()) return false;
        defaultMenu = history.pop(); menu = RadialLayouts.apply(defaultMenu);
        session.replace(menu.page()); return true;
    }
    public static boolean confirmHovered() {
        requireClientThread(); if (session == null || closing) return false;
        updateHover();
        try { return session.confirmRelease(); }
        catch (RuntimeException error) { LogUtils.getLogger().error("EsRadial action failed", error); return false; }
    }
    /** End the action session before entering a separate inert editor screen. */
    public static boolean editCurrentLayout() {
        requireClientThread();
        if (session == null || closing) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.player.isDeadOrDying()
                || !mc.isWindowActive()) return false;
        var editor = new RadialLayoutEditorScreen(defaultMenu, menu);
        close(); finishClose();
        mc.setScreen(editor);
        return true;
    }
    public static void close() {
        requireClientThread(); if (session != null) session.close(RadialSession.CloseReason.CANCEL);
    }
    public static void close(String owner) {
        if (options != null && options.owner().equals(owner)) close();
    }
    private static void beginClose(RadialSession.CloseReason reason) {
        closeFrom = animation(); closing = true; animationStart = System.nanoTime();
        try { options.onClose().accept(reason); }
        catch (RuntimeException error) { LogUtils.getLogger().error("EsRadial close listener failed", error); }
    }
    private static void finishClose() {
        Minecraft mc = Minecraft.getInstance();
        try { if (renderer != null) renderer.close(); }
        finally {
            boolean restore = restoreMouse;
            session = null; menu = defaultMenu = null; renderer = null; options = null; closing = restoreMouse = false; history.clear();
            // A newly opened Screen or a lost window owns cursor restoration now.
            if (restore && mc.player != null && mc.level != null && mc.screen == null
                    && mc.getOverlay() == null && mc.isWindowActive()) mc.mouseHandler.grabMouse();
        }
    }
    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || session == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isDeadOrDying() || mc.screen != null
                || mc.getOverlay() != null || !mc.isWindowActive()) {
            session.close(RadialSession.CloseReason.UNAVAILABLE); finishClose(); return;
        }
        if (closing) { if (animation() <= 0) finishClose(); return; }
        if (mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();
        try {
            updateHover();
            if (releaseOpeningKey()) return;
            long window = mc.getWindow().getWindow();
            session.updatePrimary(GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
        } catch (RuntimeException error) {
            LogUtils.getLogger().error("EsRadial input failed", error);
            session.close(RadialSession.CloseReason.ERROR); finishClose();
        }
    }
    private static void updateHover() {
        if (session != null && !closing) {
            if (renderer.isBackButtonHovered(menu, !history.isEmpty())) session.hover(0, 0);
            else session.hover(renderer.mouseX(), renderer.mouseY());
        }
    }
    private static boolean releaseOpeningKey() {
        if (options.heldKey() == null || options.heldKey().getAsBoolean()) return false;
        if (options.confirmOnRelease()) session.confirmRelease(); else session.close(RadialSession.CloseReason.RELEASE);
        return true;
    }
    private static void onMouse(InputEvent.MouseButton.Pre event) {
        if (session == null) return;
        event.setCanceled(true);
        if (closing) return;
        if (releaseOpeningKey()) return;
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            try {
                updateHover();
                if (event.getAction() == GLFW.GLFW_PRESS && renderer.isBackButtonHovered(menu, !history.isEmpty())) {
                    back(); session.seedPrimary(true); return;
                }
                session.updatePrimary(event.getAction() == GLFW.GLFW_PRESS);
            } catch (RuntimeException error) {
                LogUtils.getLogger().error("EsRadial action failed", error);
                session.close(RadialSession.CloseReason.ERROR);
            }
        }
    }
    private static void onKey(InputEvent.Key event) {
        if (session != null && !closing && event.getKey() == GLFW.GLFW_KEY_F6 && event.getAction() == GLFW.GLFW_PRESS) {
            editCurrentLayout(); return;
        }
        if (session != null && event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) close();
    }
    private static void render(RenderGuiEvent.Post event) {
        if (session == null) return;
        try {
            var path = new java.util.ArrayList<String>();
            history.descendingIterator().forEachRemaining(p -> path.add(p.title().getString()));
            path.add(menu.title().getString());
            renderer.setNavigation(String.join(" / ", path), !history.isEmpty());
            updateHover(); renderer.update(menu, session, animation());
            renderer.render(event.getGuiGraphics());
        } catch (RuntimeException error) {
            LogUtils.getLogger().error("EsRadial renderer failed", error);
            session.close(RadialSession.CloseReason.ERROR); finishClose();
        }
    }
    private static double animation() {
        if (menu == null) return 0;
        double durationMs = (closing ? 120 : 160) / menu.animationSpeed();
        double t = Math.min(1, Math.max(0, (System.nanoTime() - animationStart) / 1_000_000.0 / durationMs));
        double ease = 1 - Math.pow(1 - t, 3);
        return closing ? closeFrom * (1 - ease) : 0.75 + 0.25 * ease;
    }
    private static void requireClientThread() {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("EsRadial must run on the client thread");
    }
}
