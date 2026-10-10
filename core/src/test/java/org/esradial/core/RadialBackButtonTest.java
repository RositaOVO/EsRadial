package org.esradial.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class RadialBackButtonTest {
    @Test void buttonHitBoxMatchesItsVisibleExtentAndIsAboveRing() {
        for (double radius : new double[] {64,96,120}) {
            var button = RadialBackButton.above(radius,400);
            assertTrue(button.top() + button.height() < -radius);
            assertTrue(button.contains(button.left(),button.top()));
            assertTrue(button.contains(0,button.top()+10));
            assertFalse(button.contains(button.left()+button.width(),button.top()));
            assertFalse(button.contains(0,button.top()+button.height()));
            assertFalse(button.contains(Double.NaN,button.top()));
        }
    }
    @Test void buttonStaysVisibleInShortViewport() {
        var b = RadialBackButton.above(120,240);
        assertEquals(8,b.top()+120);
    }
    @Test void returningDoesNotExecuteParentActionOnHeldPrimary() {
        var actions = new AtomicInteger();
        var layout = new RadialLayout(44,96,List.of(new RadialLayout.Sector(0,360,0)));
        var parent = new RadialSession.Page<>("parent",layout,List.of(new RadialSession.Slot<>("action","action",true,false,1,actions::incrementAndGet)));
        var session = new RadialSession<>(parent,r -> {});
        session.push(new RadialSession.Page<>("child",layout,List.of(new RadialSession.Slot<>("child-action","child",true,false,1,actions::incrementAndGet))));
        session.hover(0,0); assertTrue(session.back()); session.seedPrimary(true);
        session.hover(0,-100); // Even a custom wheel that selects beyond its outer edge must remain disarmed.
        for (int i = 0; i < 20; i++) session.updatePrimary(true);
        assertEquals(0,actions.get()); assertEquals("parent",session.page().id());
        session.updatePrimary(false); session.hover(0,-60); session.updatePrimary(true);
        assertEquals(1,actions.get());
    }
}
