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
        if (changed || nextHover != hovered || Math.abs(nextProgress - progress) > 0.001
                || Math.abs(animation - this.animation) > 0.001) {
            hovered = nextHover; progress = nextProgress; this.animation = animation;
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
        ctx.setFillStyle(gradient);
        sector(ctx, c, inner, outer, 0, Math.PI * 2); ctx.fill();
        int count = session.page().slots().size();
        // A restrained segmented ring, matching Squad's interaction-wheel structure.
        ctx.setStrokeStyle("rgba(205,211,211,0.28)"); ctx.setLineWidth(1);
        for (int i = 0; count > 1 && i < count; i++) {
            double a = -Math.PI / 2 + (i - 0.5) * Math.PI * 2 / count;
            ctx.beginPath(); ctx.moveTo(c + Math.cos(a) * inner, c + Math.sin(a) * inner);
            ctx.lineTo(c + Math.cos(a) * outer, c + Math.sin(a) * outer); ctx.stroke();
        }
        ctx.beginPath(); ctx.arc(c, c, outer, 0, Math.PI * 2); ctx.stroke();
        ctx.beginPath(); ctx.arc(c, c, inner, 0, Math.PI * 2); ctx.stroke();
        if (hovered >= 0 && hovered < count) {
            var selected = session.page().slots().get(hovered);
            double step = Math.PI * 2 / count;
            double start = -Math.PI / 2 + hovered * step - step / 2;
            ctx.setFillStyle(selected.enabled() ? "rgba(222,228,230,0.24)" : "rgba(130,50,45,0.28)");
            sector(ctx, c, inner, outer, start, start + step); ctx.fill();
            ctx.setStrokeStyle("rgba(255,255,255,0.85)"); ctx.setLineWidth(1);
            sector(ctx, c, inner, outer, start, start + step); ctx.stroke();
            if (progress > 0) {
                ctx.setStrokeStyle(cssColor(selected.value().color())); ctx.setLineWidth(3);
                ctx.beginPath(); ctx.arc(c, c, outer - 2, start, start + step * progress); ctx.stroke();
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
