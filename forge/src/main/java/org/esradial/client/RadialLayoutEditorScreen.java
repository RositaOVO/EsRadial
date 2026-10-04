package org.esradial.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.esradial.core.RadialLayoutEditor;
import org.esradial.core.RadialSession;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;

/** The same persistent ApricityUI view, with drag input and inert preview actions. */
public final class RadialLayoutEditorScreen extends Screen {
    private final RadialMenuData defaults;
    private final ResourceKey<Level> dimension;
    private RadialMenuData preview;
    private RadialSession<RadialMenuData.Visual> session;
    private RadialLayoutEditor editor;
    private AuiRadialRenderer renderer;
    private int dragged = -1, boundary = -1;
    private String message = "";

    public RadialLayoutEditorScreen(RadialMenuData defaults, RadialMenuData current) {
        super(Component.literal("轮盘布局编辑"));
        this.defaults = defaults;
        dimension = net.minecraft.client.Minecraft.getInstance().level.dimension();
        editor = new RadialLayoutEditor(current.layout(), current.slots().size());
        // A preview can never place a mark, build something or invoke a mod callback.
        var inert = current.slots().stream().map(s -> new RadialSession.Slot<>(s.id(), s.value(),
            s.enabled(), false, 0, (Runnable) () -> { })).toList();
        preview = new RadialMenuData(current.id(), current.title(), editor.layout(), inert,
            current.ringColors(), current.animationSpeed());
        session = new RadialSession<>(preview.page(), reason -> { });
    }
    @Override protected void init() {
        if (renderer != null) return;
        renderer = new AuiRadialRenderer();
        try {
            if (renderer.open(preview)) return;
        } catch (RuntimeException error) { LogUtils.getLogger().error("EsRadial editor could not open", error); }
        minecraft.player.displayClientMessage(Component.literal("轮盘编辑器打开失败，请查看日志。"), false);
        onClose();
    }
    private void refresh() {
        preview = RadialLayouts.withLayout(preview, editor.layout());
        session.replace(preview.page());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (renderer == null) return;
        double x = mouseX - width / 2.0, y = mouseY - height / 2.0;
        int piece = editor.pieceAt(x, y);
        session.hover(x, y);
        String selected = piece < 0 ? "拖动扇区调位置 · 拖动分界调大小"
            : preview.layout().sectors().get(piece).slotIndex() < 0 ? "空槽 · 可拖动调整位置和大小"
            : preview.slots().get(preview.layout().sectors().get(piece).slotIndex()).value().label().getString() + " · 拖动调整位置";
        renderer.setEditorHint(message.isEmpty() ? selected : message, piece);
        renderer.setEditorDrag(dragged, x, y);
        try {
            renderer.update(preview, session, 1);
            renderer.render(graphics);
        } catch (RuntimeException error) {
            LogUtils.getLogger().error("EsRadial editor renderer failed", error); onClose();
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        double x = mouseX - width / 2.0, y = mouseY - height / 2.0;
        boundary = editor.boundaryAt(x, y);
        dragged = boundary < 0 ? editor.pieceAt(x, y) : -1;
        message = "";
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        double x = mouseX - width / 2.0, y = mouseY - height / 2.0;
        if (boundary >= 0) {
            if (Math.hypot(x, y) >= preview.layout().innerRadius() / 2)
                editor.resizeBoundary(boundary, RadialLayoutEditor.angle(x, y));
            refresh();
        }
        return true;
    }
    @Override public boolean mouseReleased(double x, double y, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragged >= 0) {
            int target = editor.pieceAt(x - width / 2.0, y - height / 2.0);
            if (target >= 0) { editor.move(dragged, target); refresh(); }
        }
        dragged = boundary = -1; return true;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        editor.rotate(delta * 5); refresh(); return true;
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            try {
                RadialLayouts.save(preview);
                minecraft.player.displayClientMessage(Component.literal("轮盘布局已保存。"), true);
                onClose();
            } catch (IOException error) {
                message = "保存失败，请检查 config/esradial/layouts.json";
                LogUtils.getLogger().warn("EsRadial editor save failed", error);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_R) {
            editor = new RadialLayoutEditor(defaults.layout(), defaults.slots().size());
            dragged = boundary = -1; message = "已恢复默认布局 · Enter保存，Esc取消";
            refresh(); return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.level == null || minecraft.player.isDeadOrDying()
                || !dimension.equals(minecraft.level.dimension()) || !minecraft.isWindowActive()) onClose();
    }
    @Override public void removed() { if (renderer != null) { renderer.close(); renderer = null; } }
    @Override public boolean isPauseScreen() { return false; }
}
