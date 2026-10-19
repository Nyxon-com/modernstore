//made with ❤ by nyxon.
package net.modern.store.util;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class MaterialHelper {

    private static final Material DEFAULT = Material.BARRIER;

    private static final Map<String, String> FALLBACKS = new HashMap<>();

    static {
        FALLBACKS.put("MUD_BRICK_SLAB",   "PACKED_MUD_BRICK_SLAB");
        FALLBACKS.put("MUD_BRICK_STAIRS", "PACKED_MUD_BRICK_STAIRS");
        FALLBACKS.put("MUD_BRICK_WALL",   "PACKED_MUD_BRICK_WALL");
        FALLBACKS.put("MUD_BRICKS",       "PACKED_MUD_BRICKS");
        FALLBACKS.put("SHORT_GRASS",      "GRASS");
        FALLBACKS.put("GRASS",            "SHORT_GRASS");
    }

    private static Logger logger;

    private MaterialHelper() {}

    public static void init(Logger pluginLogger) {
        logger = pluginLogger;
    }

    public static Material getMaterial(String name) {
        if (name == null || name.isBlank()) {
            warn("Invalid material name: null/empty — using " + DEFAULT);
            return DEFAULT;
        }

        String upper    = name.toUpperCase();
        String resolved = FALLBACKS.getOrDefault(upper, upper);

        Material mat = Material.matchMaterial(resolved);
        if (mat == null) {
            warn("Material not found: '" + name + "'"
                    + (resolved.equals(upper) ? "" : " (tried fallback '" + resolved + "')")
                    + " — using " + DEFAULT);
            return DEFAULT;
        }
        if (!resolved.equals(upper)) {
            info("Material fallback applied: " + upper + " → " + resolved);
        }
        return mat;
    }

    public static Material getMaterialOrDefault(String name, Material fallback) {
        Material result = getMaterial(name);
        return result == DEFAULT && !name.equalsIgnoreCase("BARRIER") ? fallback : result;
    }

    public static void addFallback(String oldName, String newName) {
        FALLBACKS.put(oldName.toUpperCase(), newName.toUpperCase());
    }

    private static void warn(String msg) {
        if (logger != null) logger.warning("[ModernStore/Material] " + msg);
    }

    private static void info(String msg) {
        if (logger != null) logger.info("[ModernStore/Material] " + msg);
    }
}
