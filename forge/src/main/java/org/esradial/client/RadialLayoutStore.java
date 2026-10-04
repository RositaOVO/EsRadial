package org.esradial.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.esradial.core.RadialLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-local JSON profiles keyed by menu and stable action IDs, not changing slot indices. */
public final class RadialLayoutStore {
    public record Sector(String slot, double start, double sweep) { }
    public record Profile(double innerRadius, double outerRadius, List<Sector> sectors) {
        public boolean matches(List<String> ids) {
            if (sectors == null) return false;
            List<String> slots = sectors.stream().filter(s -> s != null && s.slot() != null)
                .map(Sector::slot).toList();
            return slots.size() == ids.size() && new HashSet<>(slots).equals(new HashSet<>(ids));
        }
        public RadialLayout resolve(List<String> ids) {
            if (!matches(ids)) throw new IllegalArgumentException("Action IDs changed");
            List<RadialLayout.Sector> areas = new ArrayList<>();
            for (Sector sector : sectors) {
                if (sector == null) throw new IllegalArgumentException("Null sector");
                int index = sector.slot() == null ? -1 : ids.indexOf(sector.slot());
                areas.add(new RadialLayout.Sector(sector.start(), sector.sweep(), index));
            }
            RadialLayout layout = new RadialLayout(innerRadius, outerRadius, areas);
            layout.validateSlots(ids.size()); return layout;
        }
    }
    private record FileData(int version, Map<String, List<Profile>> menus) { }
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private final Path path;
    public RadialLayoutStore(Path path) { this.path = path; }
    public Path path() { return path; }

    /** Reload on open, also exporting a new default catalog so users can discover its IDs. */
    public RadialLayout load(String menuId, RadialLayout fallback, List<String> ids) throws IOException {
        Map<String, List<Profile>> menus = read();
        for (Profile profile : menus.getOrDefault(menuId, List.of())) {
            if (profile.matches(ids)) {
                try { return profile.resolve(ids); }
                catch (IllegalArgumentException error) { throw new IOException("Invalid layout for " + menuId, error); }
            }
        }
        put(menus, menuId, profile(fallback, ids), ids); write(menus); return fallback;
    }
    public void save(String menuId, RadialLayout layout, List<String> ids) throws IOException {
        // Re-read immediately before writing to preserve edits to other menus made outside the game.
        Map<String, List<Profile>> menus = read();
        put(menus, menuId, profile(layout, ids), ids); write(menus);
    }
    private static Profile profile(RadialLayout layout, List<String> ids) {
        layout.validateSlots(ids.size());
        return new Profile(layout.innerRadius(), layout.outerRadius(), layout.sectors(ids.size()).stream()
            .map(s -> new Sector(s.slotIndex() < 0 ? null : ids.get(s.slotIndex()), s.startDegrees(), s.sweepDegrees())).toList());
    }
    private static void put(Map<String, List<Profile>> menus, String id, Profile profile, List<String> ids) {
        List<Profile> profiles = new ArrayList<>(menus.getOrDefault(id, List.of()));
        profiles.removeIf(p -> p.matches(ids)); profiles.add(profile); menus.put(id, profiles);
    }
    private Map<String, List<Profile>> read() throws IOException {
        if (!Files.exists(path)) return new LinkedHashMap<>();
        if (Files.size(path) > 4 * 1024 * 1024) throw new IOException("Layout file exceeds 4 MiB");
        try {
            var root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("version") || root.get("version").getAsInt() != 1 || !root.has("menus") || !root.get("menus").isJsonObject())
                throw new IllegalArgumentException("Expected version=1 and menus object");
            FileData data = GSON.fromJson(root, FileData.class);
            for (var entry : data.menus().entrySet()) {
                if (entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().stream().anyMatch(p -> p == null || p.sectors() == null))
                    throw new IllegalArgumentException("Invalid profile list");
            }
            return new LinkedHashMap<>(data.menus());
        } catch (RuntimeException error) { throw new IOException("Invalid JSON layout file: " + path, error); }
    }
    private void write(Map<String, List<Profile>> menus) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "layouts-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(new FileData(1, menus)) + "\n", StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (RuntimeException error) { throw new IOException("Unable to serialize layout file: " + path, error); }
        finally { Files.deleteIfExists(temporary); }
    }
}
