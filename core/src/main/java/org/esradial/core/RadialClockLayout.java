package org.esradial.core;

import java.util.ArrayList;

/** Thirty-degree placement guides, not twelve reserved action slots. */
public final class RadialClockLayout {
    private RadialClockLayout() { }
    public static RadialLayout create(double inner, double outer, int count, int backIndex) {
        if (count < 0 || count > 64 || backIndex < -1 || backIndex >= count)
            throw new IllegalArgumentException("Invalid clock menu");
        int actions = count - (backIndex >= 0 ? 1 : 0);
        // Larger legacy menus retain an equal layout; new catalogues paginate at eleven actions.
        if (actions > (backIndex >= 0 ? 11 : 12)) return new RadialLayout(inner, outer);
        var sectors = new ArrayList<RadialLayout.Sector>();
        int action = 0;
        for (int i = 0; i < count; i++) if (i != backIndex)
            sectors.add(new RadialLayout.Sector(action++ * 30, 30, i));
        if (backIndex >= 0) {
            double start = Math.max(270, actions * 30);
            sectors.add(new RadialLayout.Sector(start, 360 - start, backIndex));
        }
        return new RadialLayout(inner, outer, sectors);
    }
}
