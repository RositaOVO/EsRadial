package org.esradial.client.toast;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import org.esradial.core.ToastTimeline;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-docked slide-in toasts.
 *
 * <p>One toast is shown at a time; showing a new one replaces the current one. The toast is drawn
 * on the HUD when no screen is open, and above the open screen otherwise, so messages raised from
 * menus stay visible. Client side only.</p>
 *
 * <pre>{@code
 * EsToasts.show(ToastSpec.error(Component.literal("无法补给"), Component.literal("补给点已耗尽")));
 * }</pre>
 */
public final class EsToasts {
    /** Lift above screen widgets when drawing over an open screen. */
    private static final float SCREEN_Z = 1200F;
    private static final int PADDING = 8;
    private static final int TITLE_GAP = 4;

    private static boolean initialized;
    private static Active active;

    private record Active(ToastSpec spec, ToastTimeline timeline) { }

    private EsToasts() { }

    /** Registers render listeners. Called by EsRadial on client start; safe to call again. */
    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, EsToasts::onRenderGui);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, EsToasts::onRenderScreen);
    }

    /** Shows {@code spec}, replacing any toast currently on screen. May be called from any thread. */
    public static void show(ToastSpec spec) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(() -> show(spec));
            return;
        }
        initialize();
        active = new Active(spec, new ToastTimeline(spec.slideMillis(), spec.holdTicks() * 50L, Util.getMillis()));
    }

    /** Removes the current toast immediately. */
    public static void clear() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isSameThread()) {
            mc.execute(EsToasts::clear);
            return;
        }
        active = null;
    }

    public static boolean isActive() {
        Active current = active;
        return current != null && !current.timeline().isDone(Util.getMillis());
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen != null) return; // drawn by onRenderScreen instead
        render(event.getGuiGraphics(), Double.NaN, Double.NaN, false);
    }

    private static void onRenderScreen(ScreenEvent.Render.Post event) {
        render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), true);
    }

    private static void render(GuiGraphics graphics, double mouseX, double mouseY, boolean overScreen) {
        Active current = active;
        if (current == null) return;
        long now = Util.getMillis();
        ToastTimeline timeline = current.timeline();
        if (timeline.isDone(now)) {
            if (active == current) active = null;
            return;
        }
        ToastSpec spec = current.spec();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int w = Math.max(1, Math.min(spec.width(), screenW - spec.margin() * 2));
        int h = Math.max(1, Math.min(spec.height(), screenH - spec.margin() * 2));
        int restX = screenW - spec.margin() - w;
        int hiddenX = screenW + spec.offscreenPadding();
        int y = (screenH - h) / 2;

        boolean hovered = spec.pauseOnHover() && !Double.isNaN(mouseX)
                && mouseX >= restX && mouseX < restX + w && mouseY >= y && mouseY < y + h;
        timeline.setHovered(hovered, now);

        float shown = timeline.visibility(now);
        int x = Math.round(hiddenX + (restX - hiddenX) * shown);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (overScreen) pose.translate(0F, 0F, SCREEN_Z);
        draw(graphics, spec, x, y, w, h);
        pose.popPose();
    }

    private static void draw(GuiGraphics graphics, ToastSpec spec, int x, int y, int w, int h) {
        Font font = Minecraft.getInstance().font;
        graphics.fill(x, y, x + w, y + h, spec.backgroundArgb());
        if (spec.stripeWidth() > 0) graphics.fill(x, y, x + spec.stripeWidth(), y + h, spec.accentArgb());

        int textX = x + spec.stripeWidth() + PADDING;
        int textW = Math.max(1, w - spec.stripeWidth() - PADDING * 2);
        int textY = y + PADDING;
        int bottom = y + h - PADDING;

        List<FormattedCharSequence> titleLines = fitLines(font, spec.title(), textW, 1);
        if (!titleLines.isEmpty() && !spec.title().getString().isEmpty()) {
            graphics.drawString(font, titleLines.get(0), textX, textY, spec.accentArgb(), false);
            textY += font.lineHeight + TITLE_GAP;
        }

        int lineStep = font.lineHeight + 1;
        int maxLines = Math.max(0, (bottom - textY + 1) / lineStep);
        List<FormattedCharSequence> bodyLines = fitLines(font, spec.body(), textW, maxLines);
        for (FormattedCharSequence line : bodyLines) {
            graphics.drawString(font, line, textX, textY, spec.bodyArgb(), false);
            textY += lineStep;
        }
    }

    /** Wraps {@code body} to {@code width}; if it needs more than {@code maxLines}, ends the last line with "…". */
    private static List<FormattedCharSequence> fitLines(Font font, Component body, int width, int maxLines) {
        if (maxLines <= 0) return List.of();
        List<FormattedText> lines = font.getSplitter().splitLines(body, width, Style.EMPTY);
        List<FormattedCharSequence> out = new ArrayList<>(Math.min(lines.size(), maxLines));
        if (lines.size() <= maxLines) {
            for (FormattedText line : lines) out.add(Language.getInstance().getVisualOrder(line));
            return out;
        }
        for (int i = 0; i < maxLines - 1; i++) out.add(Language.getInstance().getVisualOrder(lines.get(i)));
        FormattedText last = lines.get(maxLines - 1);
        FormattedText ellipsis = FormattedText.of("…");
        FormattedText clipped = font.substrByWidth(last, Math.max(0, width - font.width(ellipsis)));
        out.add(Language.getInstance().getVisualOrder(FormattedText.composite(clipped, ellipsis)));
        return out;
    }
}
