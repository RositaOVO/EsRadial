package org.esradial.core;

/** A caption rectangle must fit the annulus and its own sector, including blank neighbors. */
public record RadialCaptionBounds(double left, double top, double width, double height, double iconOffsetY) {
    public static RadialCaptionBounds forSlot(RadialLayout layout, int slot, int count, double animation) {
        double x=layout.slotX(slot,count)*animation,y=layout.slotY(slot,count)*animation;
        // Prefer below; a narrow lower-side sector may fit much more text above the icon.
        var below=candidate(layout,slot,count,animation,x,y+4,-8);
        var above=candidate(layout,slot,count,animation,x,y-15,8);
        return above.width>below.width?above:below;
    }
    private static RadialCaptionBounds candidate(RadialLayout layout,int slot,int count,double animation,
                                                 double x,double top,double iconOffset) {
        double width=72,height=11;
        while(width>=16&&!fits(layout,slot,count,x-width/2,top,width,height,animation))width-=2;
        if(width<16)width=0;
        return new RadialCaptionBounds(x-width/2,top,width,height,iconOffset);
    }
    private static boolean fits(RadialLayout layout, int slot, int count, double left, double top,
                                double width, double height, double animation) {
        double closestX = Math.max(left, Math.min(0, left + width));
        double closestY = Math.max(top, Math.min(0, top + height));
        if (Math.hypot(closestX, closestY) < layout.innerRadius() * animation + 2) return false;
        for (double x : new double[] {left,left + width}) for (double y : new double[] {top,top + height}) {
            if (Math.hypot(x,y) > layout.outerRadius() * animation - 3) return false;
            // hitIndex is directional outside the inner dead zone: undo animation for that check.
            if (layout.hitIndex(x / animation,y / animation,count) != slot) return false;
        }
        return true;
    }
}
