package org.esradial.client;

import org.esradial.core.RadialLayout;
import org.esradial.core.RadialLayoutEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RadialLayoutStoreTest {
    @TempDir Path folder;
    @Test void defaultsAreExportedAndSavedPositionsFollowIdsWhenCatalogReorders() throws Exception {
        var store = new RadialLayoutStore(folder.resolve("esradial/layouts.json"));
        var ids = List.of("build", "ammo", "rally");
        var original = RadialLayout.squad(44, 96, 3);
        assertEquals(original, store.load("espetro:build", original, ids));
        assertTrue(Files.readString(store.path()).contains("\"build\""));
        var editor = new RadialLayoutEditor(original, 3);
        editor.move(0, 3); editor.rotate(40);
        store.save("espetro:build", editor.layout(), ids);
        var reordered = store.load("espetro:build", original, List.of("rally", "build", "ammo"));
        assertEquals(editor.layout().sectorForSlot(0, 3).startDegrees(), reordered.sectorForSlot(1, 3).startDegrees());
        assertEquals(editor.layout().sectorForSlot(2, 3).startDegrees(), reordered.sectorForSlot(0, 3).startDegrees());
    }
    @Test void differentPagesAndPermissionCatalogsKeepIndependentProfiles() throws Exception {
        var store = new RadialLayoutStore(folder.resolve("layouts.json"));
        var layout = RadialLayout.squad(44, 96, 2);
        var editor = new RadialLayoutEditor(layout, 2); editor.rotate(70);
        store.save("espetro:skills", editor.layout(), List.of("skillA", "skillB"));
        store.load("espetro:skills", RadialLayout.squad(44, 96, 1), List.of("skillA"));
        store.load("espoints:mark", layout, List.of("attack", "defend"));
        assertEquals(editor.layout(), store.load("espetro:skills", layout, List.of("skillA", "skillB")));
    }
    @Test void manualEditsReloadWithoutRestart() throws Exception {
        var store = new RadialLayoutStore(folder.resolve("layouts.json"));
        var layout = RadialLayout.squad(44, 96, 1);
        store.load("test:wheel", layout, List.of("attack"));
        String file = Files.readString(store.path()).replace("\"innerRadius\": 44.0", "\"innerRadius\": 50.0");
        Files.writeString(store.path(), file);
        assertEquals(50, store.load("test:wheel", layout, List.of("attack")).innerRadius());
    }
    @Test void malformedFileIsNeverOverwrittenByOpenOrSave() throws Exception {
        var store = new RadialLayoutStore(folder.resolve("layouts.json"));
        Files.writeString(store.path(), "{bad json");
        assertThrows(IOException.class, () -> store.load("test:wheel", RadialLayout.squad(44, 96, 1), List.of("attack")));
        assertThrows(IOException.class, () -> store.save("test:wheel", RadialLayout.squad(44, 96, 1), List.of("attack")));
        assertEquals("{bad json", Files.readString(store.path()));
    }
    @Test void overlappingAndDuplicateSectorsAreRejected() {
        var overlap = new RadialLayoutStore.Profile(44, 96, List.of(
            new RadialLayoutStore.Sector("attack", 0, 100), new RadialLayoutStore.Sector("defend", 50, 50)));
        assertThrows(IllegalArgumentException.class, () -> overlap.resolve(List.of("attack", "defend")));
        var duplicate = new RadialLayoutStore.Profile(44, 96, List.of(
            new RadialLayoutStore.Sector("attack", 0, 100), new RadialLayoutStore.Sector("attack", 100, 50)));
        assertFalse(duplicate.matches(List.of("attack", "defend")));
    }
    @Test void nonFiniteValueInAnotherProfileCannotCrashOrOverwriteTheFile() throws Exception {
        var store = new RadialLayoutStore(folder.resolve("layouts.json"));
        String file = """
            {"version":1,"menus":{"broken:wheel":[{"innerRadius":"NaN","outerRadius":96,
            "sectors":[{"slot":"broken","start":0,"sweep":360}]}]}}
            """;
        Files.writeString(store.path(), file);
        assertThrows(IOException.class, () -> store.load("test:wheel", RadialLayout.squad(44, 96, 1), List.of("attack")));
        assertEquals(file, Files.readString(store.path()));
    }
}
