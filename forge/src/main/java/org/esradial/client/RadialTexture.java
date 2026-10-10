package org.esradial.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sighs.apricityui.element.Texture;
import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.render.Base;
import com.sighs.apricityui.render.ImageDrawer;
import com.sighs.apricityui.render.Rect;

/** AUI's regular texture path with an optional vertex tint, without changing source artwork. */
final class RadialTexture extends Texture {
    private int tint;
    RadialTexture(Document document,int tint) { super(document); this.tint=tint; }
    void setTint(int tint) { this.tint=tint; }
    @Override public void drawPhase(PoseStack pose,Base.RenderPhase phase) {
        if(phase!=Base.RenderPhase.BODY || tint==0xFFFFFFFF) { super.drawPhase(pose,phase); return; }
        var rect=Rect.of(this);rect.drawBody(pose);
        var texture=getTextureLocation();if(texture==null) return;
        var p=rect.getBodyRectPosition();var size=rect.getBodyRectSize();
        if(size.width()<=0 || size.height()<=0) return;
        ImageDrawer.draw(pose,texture,(float)p.x,(float)p.y,(float)size.width(),(float)size.height(),
            "true".equals(getAttribute("blur")),tint);
    }
}
