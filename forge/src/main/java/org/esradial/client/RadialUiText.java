package org.esradial.client;

import com.sighs.apricityui.layout.Position;
import com.sighs.apricityui.parser.Color;
import com.sighs.apricityui.render.FontDrawer;
import com.sighs.apricityui.style.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** One smooth font for the AUI wheel and its map coordinate overlay. */
public final class RadialUiText {
    public static final String FAMILY = "'Microsoft YaHei','Noto Sans CJK SC',sans-serif";
    private RadialUiText() { }
    private static Text style(double size, int weight) {
        var text = new Text(); text.fontFamily = FAMILY; text.fontSize = size; text.fontWeight = weight;
        text.lineHeight = size + 2; text.whiteSpace = "nowrap"; return text;
    }
    public static double width(String source, double size, int weight) {
        return source == null ? 0 : Text.measureLine(style(size,weight), source);
    }
    public static String fit(String source, double width, double size, int weight) {
        if (source == null || width <= 0) return "";
        if (width(source,size,weight) <= width) return source;
        if (width("…",size,weight) > width) return "";
        String shorter = source;
        while (!shorter.isEmpty() && width(shorter+"…",size,weight) > width)
            shorter = shorter.substring(0,shorter.offsetByCodePoints(shorter.length(),-1));
        return shorter+"…";
    }
    public static void draw(GuiGraphics graphics, String source, double x, double y, int argb, double size, int weight) {
        if (source == null || source.isEmpty()) return;
        // AUI renders immediately; flush queued vanilla backgrounds before the glyphs.
        graphics.flush();
        var text = style(size,weight); text.content = source; text.color = new Color(argb);
        var window = Minecraft.getInstance().getWindow();
        FontDrawer.pushDocumentPixelScale(window.getWidth()/(double)window.getGuiScaledWidth());
        try { FontDrawer.drawFontOnBaseline(graphics.pose(),text,new Position(x,y),size); }
        finally { FontDrawer.popDocumentPixelScale(); }
    }
    public static void beginFrame() { FontDrawer.drainCompletedRasters(); }
}
