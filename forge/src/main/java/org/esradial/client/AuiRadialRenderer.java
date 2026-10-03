package org.esradial.client;

import com.sighs.apricityui.ApricityUI;
import com.sighs.apricityui.canvas.CanvasRenderingContext2D;
import com.sighs.apricityui.element.Canvas;
import com.sighs.apricityui.element.Texture;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.layout.Position;
import com.sighs.apricityui.layout.Size;
import com.sighs.apricityui.render.Base;
import net.minecraft.client.gui.GuiGraphics;
import org.esradial.core.RadialSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** ApricityUI draws the ring, textures and labels; only item/custom icons use Minecraft. */
public final class AuiRadialRenderer implements RadialRenderer {
    public static final String DEFAULT_TEMPLATE = "esradial/radial.html";
    private final String template;
    private Document document;
    private RadialMenuData menu;
    private RadialSession<RadialMenuData.Visual> session;
    private final Map<String, Element> nodes = new LinkedHashMap<>();
    private Element root, slots, label, denial;
    private Canvas canvas;
    private long generation = -1, paintedRevision = -1;
    private int hovered = -2;
    private double width = -1, height = -1, animation = -1, progress = -1;
    private RadialMenuData.Progress externalProgress = RadialMenuData.Progress.NONE;

    public AuiRadialRenderer() { this(DEFAULT_TEMPLATE); }
    public AuiRadialRenderer(String template) { this.template = template; }
    public boolean open(RadialMenuData menu) {
        this.menu = menu;
        document = ApricityUI.createDocument(template);
        if (document == null) return false;
        if (!bind()) { close(); return false; }
        return true;
    }
    private boolean bind() {
        document.setManuallyRendered(true);
        root = document.getElementById("radial"); slots = document.getElementById("slots");
        label = document.getElementById("label"); denial = document.getElementById("denial");
        Element element = document.getElementById("ring");
        if (root == null || slots == null || label == null || denial == null || !(element instanceof Canvas)) return false;
        canvas = (Canvas) element; generation = document.getRefreshGeneration();
        paintedRevision = -1; hovered = -2; width = height = -1; animation = progress = -1; nodes.clear();
        return true;
    }
    public double mouseX() {
        Position p = document.getMouseDocumentPosition(); return p.x - document.getViewportSize().width() / 2;
    }
    public double mouseY() {
        Position p = document.getMouseDocumentPosition(); return p.y - document.getViewportSize().height() / 2;
    }
    public void update(RadialMenuData menu, RadialSession<RadialMenuData.Visual> session, double animation) {
        if (document == null || !document.isActive()) throw new IllegalStateException("AUI document is unavailable");
        if (generation != document.getRefreshGeneration() && !bind()) throw new IllegalStateException("Invalid radial template after reload");
        this.menu = menu; this.session = session;
        Size size = document.getViewportSize();
        boolean resized = width != size.width() || height != size.height();
        boolean changed = resized || paintedRevision != session.revision();
        if (changed) {
            width = size.width(); height = size.height();
            Document.runWithContext(document, () -> syncSlots(animation));
            paintedRevision = session.revision();
        }
        int nextHover = session.hoveredIndex();
        double nextProgress = session.repeatProgress();
        var nextExternalProgress = Objects.requireNonNullElse(menu.progress().get(), RadialMenuData.Progress.NONE);
        if (changed || nextHover != hovered || Math.abs(nextProgress - progress) > 0.001
                || !nextExternalProgress.equals(externalProgress)
                || Math.abs(animation - this.animation) > 0.001) {
            hovered = nextHover; progress = nextProgress; this.animation = animation;
            externalProgress = nextExternalProgress;
            Document.runWithContext(document, () -> {
                updateLabels(); positionIcons(animation); drawRing(animation);
            });
        }
    }
    private void syncSlots(double animation) {
        var ids = session.page().slots().stream().map(RadialSession.Slot::id).toList();
        for (String id : new ArrayList<>(nodes.keySet())) if (!ids.contains(id)) nodes.remove(id).remove();
        for (var slot : session.page().slots()) {
            Element node = nodes.get(slot.id());
            if (node == null) {
                node = document.createElement("div"); node.setAttribute("data-id", slot.id());
                node.setAttribute("class", "radial-slot"); slots.appendChild(node); nodes.put(slot.id(), node);
            }
            var visual = slot.value();
            String source = visual.texture() == null ? "" : visual.texture().toString();
            if (!source.equals(node.getAttribute("data-texture"))) {
                for (Element child : new ArrayList<>(node.children)) child.remove();
                if (!source.isEmpty()) {
                    // Document.createElement creates a plain Element, even for registered tags.
                    // Instantiate the AUI texture renderer so the source is actually drawn.
                    Element texture = new Texture(document); texture.setAttribute("src", source);
                    node.appendChild(texture);
                }
                node.setAttribute("data-texture", source);
            }
        }
        positionIcons(animation);
    }
    private void positionIcons(double animation) {
        int count = session.page().slots().size();
        for (int i = 0; i < count; i++) {
            var slot = session.page().slots().get(i); Element node = nodes.get(slot.id());
            double x = width / 2 + menu.layout().slotX(i, count) * animation;
            double y = height / 2 + menu.layout().slotY(i, count) * animation;
            node.setAttribute("style", "left:" + (x - 16) + "px;top:" + (y - 16) + "px;opacity:"
                    + animation * (slot.enabled() ? 1 : 0.35) + ";color:" + cssColor(slot.value().color()) + ";");
            node.setAttribute("class", "radial-slot" + (i == hovered ? " hovered" : "") + (!slot.enabled() ? " disabled" : ""));
        }
    }
    private void updateLabels() {
        var selected = session.hovered();
        label.setTextContent(selected == null ? menu.title().getString() : selected.value().label().getString());
        denial.setAttribute("class", "radial-denial" + (selected != null && !selected.enabled() ? " unavailable" : ""));
        denial.setTextContent(selected != null && !selected.enabled() ? selected.value().denial().getString()
            : selected != null && selected.repeatTicks() > 0 ? "按住左键持续操作 · 右键返回"
            : "左键选择 · 右键返回/关闭");
        double labelTop = menu.layout().outerRadius() + 16;
        // Keep all overlay positions in the same document coordinate space as the ring.
        // AUI absolute positioning does not match browser negative-margin centering.
        String labelLeft = "left:" + (width - 240) / 2 + "px;";
        label.setAttribute("style", labelLeft + "top:" + (height / 2 + labelTop) + "px;margin:0;");
        denial.setAttribute("style", labelLeft + "top:" + (height / 2 + labelTop + 28) + "px;margin:0;");
        root.setAttribute("style", "opacity:" + this.animation + ";");
    }
    private void drawRing(double animation) {
        int extent = (int) Math.ceil(menu.layout().outerRadius() * 2 + 8);
        if (canvas.getWidth() != extent) canvas.setWidth(extent);
        if (canvas.getHeight() != extent) canvas.setHeight(extent);
        canvas.setAttribute("style", "width:" + extent + "px;height:" + extent + "px;left:"
                + (width - extent) / 2 + "px;top:" + (height - extent) / 2 + "px;");
        CanvasRenderingContext2D ctx = canvas.getContext("2d");
        ctx.clearRect(0, 0, extent, extent);
        double c = extent / 2.0, inner = menu.layout().innerRadius() * animation, outer = menu.layout().outerRadius() * animation;
        if (outer < 1) return;
        var gradient = ctx.createRadialGradient(c, c, inner, c, c, outer);
        for (int i = 0; i < menu.ringColors().size(); i++)
            gradient.addColorStop(menu.ringColors().size() == 1 ? 0 : i / (double) (menu.ringColors().size() - 1), cssColor(menu.ringColors().get(i)));
        int count = session.page().slots().size();
        var sectors = menu.layout().sectors(count);
        var blankGradient = ctx.createRadialGradient(c, c, inner, c, c, outer);
        blankGradient.addColorStop(0, "rgba(36,41,43,0.37)");
        blankGradient.addColorStop(1, "rgba(50,56,58,0.43)");
        for (var area : sectors) {
            ctx.setFillStyle(area.slotIndex() < 0 ? blankGradient : gradient);
            sector(ctx, c, inner, outer, area.startRadians(), area.endRadians()); ctx.fill();
        }
        ctx.setStrokeStyle("rgba(213,222,216,0.27)"); ctx.setLineWidth(0.6);
        for (var area : sectors) {
            if (area.sweepDegrees() >= 360) continue;
            double a = area.startRadians();
            ctx.beginPath(); ctx.moveTo(c + Math.cos(a) * inner, c + Math.sin(a) * inner);
            ctx.lineTo(c + Math.cos(a) * outer, c + Math.sin(a) * outer); ctx.stroke();
        }
        // The outer outline stays visible even with no selection or active operation.
        ctx.setStrokeStyle("rgba(226,234,229,0.8)"); ctx.setLineWidth(0.75);
        ctx.beginPath(); ctx.arc(c, c, outer, 0, Math.PI * 2); ctx.stroke();
        ctx.setStrokeStyle("rgba(216,225,219,0.52)"); ctx.setLineWidth(0.65);
        ctx.beginPath(); ctx.arc(c, c, inner, 0, Math.PI * 2); ctx.stroke();
        // The center track hugs the inside edge. A game-owned progress can continue here
        // without a hovered button; repeat timing still belongs to the session alone.
        double trackRadius = Math.max(0, inner - 2);
        boolean progressOutside = hovered >= 0 && externalProgress.slotId() != null
                && session.page().slots().get(hovered).id().equals(externalProgress.slotId());
        if (!progressOutside && !(hovered >= 0 && progress > 0) && trackRadius > 0) {
            ctx.setStrokeStyle("rgba(231,239,233,0.28)"); ctx.setLineWidth(1.5);
            ctx.beginPath(); ctx.arc(c, c, trackRadius, 0, Math.PI * 2); ctx.stroke();
            if (externalProgress.value() > 0) {
                ctx.setStrokeStyle(cssColor(externalProgress.color()));
                ctx.beginPath(); ctx.arc(c, c, trackRadius, -Math.PI / 2,
                        -Math.PI / 2 + Math.PI * 2 * externalProgress.value()); ctx.stroke();
            }
        }
        if (hovered >= 0 && hovered < count) {
            var selected = session.page().slots().get(hovered);
            var area = menu.layout().sectorForSlot(hovered, count);
            double start = area.startRadians(), end = area.endRadians();
            ctx.setFillStyle(selected.enabled() ? "rgba(222,228,230,0.24)" : "rgba(130,50,45,0.28)");
            sector(ctx, c, inner, outer, start, end); ctx.fill();
            ctx.setStrokeStyle("rgba(247,249,248,0.85)"); ctx.setLineWidth(0.7);
            for (double angle : new double[]{start, end}) {
                ctx.beginPath(); ctx.moveTo(c + Math.cos(angle) * inner, c + Math.sin(angle) * inner);
                ctx.lineTo(c + Math.cos(angle) * outer, c + Math.sin(angle) * outer); ctx.stroke();
            }
            // Keep the whole selected arc lit, including before pressing the mouse.
            ctx.setStrokeStyle(selected.enabled() ? cssColor(selected.value().color()) : "rgba(255,119,119,0.5)");
            ctx.setLineWidth(1.5);
            ctx.beginPath(); ctx.arc(c, c, Math.max(0, outer - 1), start, end); ctx.stroke();
            double value = progressOutside ? externalProgress.value() : progress;
            if (selected.enabled() && value > 0) {
                ctx.setStrokeStyle(progressOutside ? cssColor(externalProgress.color()) : cssColor(selected.value().color()));
                ctx.setLineWidth(2.5);
                ctx.beginPath(); ctx.arc(c, c, Math.max(0, outer - 1), start, start + (end - start) * value); ctx.stroke();
            }
        }
    }
    private static void sector(CanvasRenderingContext2D ctx, double c, double inner, double outer, double start, double end) {
        ctx.beginPath(); ctx.arc(c, c, outer, start, end);
        ctx.arc(c, c, inner, end, start, true); ctx.closePath();
    }
    /** Minecraft-style #AARRGGBB is converted to CSS rgba; #RRGGBB remains valid CSS. */
    public static String cssColor(String color) {
        if (color != null && color.matches("#[0-9a-fA-F]{8}")) {
            long argb = Long.parseLong(color.substring(1), 16);
            return "rgba(" + ((argb >> 16) & 255) + "," + ((argb >> 8) & 255) + "," + (argb & 255)
                    + "," + ((argb >> 24) & 255) / 255.0 + ")";
        }
        return color == null ? "#ffffff" : color;
    }
    public void render(GuiGraphics graphics) {
        if (document == null || session == null || animation <= 0) return;
        // Draw the updated DOM through ApricityUI before the corresponding native icons.
        Base.drawOverlayDocument(graphics.pose(), document);
        int count = session.page().slots().size();
        for (int i = 0; i < count; i++) {
            var slot = session.page().slots().get(i); var v = slot.value();
            if (v.item().isEmpty() && v.nativeIcon() == null) continue;
            Position center = document.documentToGuiPosition(new Position(width / 2 + menu.layout().slotX(i, count) * animation,
                    height / 2 + menu.layout().slotY(i, count) * animation));
            Position unit = document.documentToGuiPosition(new Position(1, 0));
            Position zero = document.documentToGuiPosition(new Position(0, 0));
            float scale = (float) (unit.x - zero.x);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(center.x, center.y, 0); graphics.pose().scale(scale, scale, 1);
                if (v.nativeIcon() != null) v.nativeIcon().render(graphics, 0, 0, (float) animation,
                        (float) animation * (slot.enabled() ? 1 : 0.35f));
                else {
                    graphics.pose().scale((float) animation * 1.5f, (float) animation * 1.5f, 1);
                    graphics.renderItem(v.item(), -8, -8);
                }
            } finally { graphics.pose().popPose(); }
        }
    }
    public void close() {
        if (document != null) document.remove();
        document = null; session = null; menu = null; nodes.clear();
    }
}
