package org.esradial.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RadialLayoutEditorTest {
    @Test void roundingTailAtZeroIsNotAnOverlap() {
        assertDoesNotThrow(() -> new RadialLayout(44, 96, List.of(
            new RadialLayout.Sector(350, 10.0000000000001, 0),
            new RadialLayout.Sector(0, 350, 1))));
    }
    @Test void movingBlankSectorPreservesActionsAndWidths() {
        var editor = new RadialLayoutEditor(RadialLayout.squad(44, 96, 7), 7);
        var before = editor.layout().sectors();
        int gap = -1;
        for (int i = 0; i < before.size(); i++) if (before.get(i).slotIndex() == -1) { gap = i; break; }
        editor.move(gap, 0);
        var after = editor.layout().sectors();
        assertEquals(-1, after.get(0).slotIndex());
        assertEquals(before.get(gap).sweepDegrees(), after.get(0).sweepDegrees());
        assertEquals(before.stream().map(RadialLayout.Sector::sweepDegrees).sorted().toList(),
            after.stream().map(RadialLayout.Sector::sweepDegrees).sorted().toList());
        after.forEach(s -> assertEquals(s.slotIndex(), editor.layout().hitIndex(
            Math.sin(s.centerRadians()) * 70, -Math.cos(s.centerRadians()) * 70, 7)));
    }
    @Test void movingActionDoesNotChangeItsStableIndex() {
        var editor = new RadialLayoutEditor(RadialLayout.squad(44, 96, 5), 5);
        int index = editor.layout().sectors().get(0).slotIndex();
        editor.move(0, 4);
        assertEquals(index, editor.layout().sectors().get(4).slotIndex());
        editor.layout().validateSlots(5);
    }
    @Test void resizeAcrossZeroPreservesCoverageAndOtherBoundaries() {
        var editor = new RadialLayoutEditor(new RadialLayout(44, 96, List.of(
            new RadialLayout.Sector(350, 30, 0), new RadialLayout.Sector(20, 60, 1),
            new RadialLayout.Sector(80, 270, -1))), 2);
        editor.resizeBoundary(0, 10);
        var layout = editor.layout();
        assertEquals(10, layout.sectors().get(0).startDegrees(), 1e-8);
        assertEquals(350, layout.sectorForSlot(0, 2).startDegrees(), 1e-8);
        assertEquals(360, layout.sectors().stream().mapToDouble(RadialLayout.Sector::sweepDegrees).sum(), 1e-8);
    }
    @Test void extremeResizeNeverInvertsAdjacentSectors() {
        var editor = new RadialLayoutEditor(RadialLayout.squad(44, 96, 7), 7);
        for (int i = 0; i < editor.layout().sectors().size(); i++) {
            editor.resizeBoundary(i, editor.layout().sectors().get(i).startDegrees() + 179);
            editor.layout().validateSlots(7);
            assertTrue(editor.layout().sectors().stream().allMatch(s -> s.sweepDegrees() >= 4 - 1e-8));
        }
    }
    @Test void rotateAndReorderAllCatalogSizesMaintainCoverage() {
        for (int n = 0; n <= 64; n++) {
            var editor = new RadialLayoutEditor(RadialLayout.squad(44, 96, n), n);
            editor.rotate(355);
            assertDoesNotThrow(editor::layout, "catalog size " + n);
            editor.move(0, editor.layout().sectors().size() - 1);
            editor.rotate(-725);
            editor.layout().validateSlots(n);
            assertEquals(360, editor.layout().sectors().stream().mapToDouble(RadialLayout.Sector::sweepDegrees).sum(), 1e-8);
        }
    }
    @Test void centerAndOutsideNeverStartDragging() {
        var editor = new RadialLayoutEditor(RadialLayout.squad(44, 96, 7), 7);
        assertEquals(-1, editor.pieceAt(0, 0));
        assertEquals(-1, editor.boundaryAt(0, 0));
        assertEquals(-1, editor.pieceAt(200, 0));
        var boundary = editor.layout().sectors().get(0);
        assertEquals(0, editor.boundaryAt(Math.sin(Math.toRadians(boundary.startDegrees())) * 96,
            -Math.cos(Math.toRadians(boundary.startDegrees())) * 96));
    }
}
