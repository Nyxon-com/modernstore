//made with ❤ by nyxon.
package net.modern.store.config;

import net.modern.store.ModernStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final ModernStore plugin;

    private String prefix;
    private long   rotationMillis;
    private String currency;
    private String defaultPool;

    private boolean broadcastEnabled;
    private boolean broadcastRotation;
    private boolean broadcastWarnings;

    private PlaceholderItem hiddenItem;
    private PlaceholderItem vipItem;
    private PlaceholderItem soldOutItem;

    private final Map<String, String>  messages = new HashMap<>();
    private final Map<Integer, String> warnings = new HashMap<>();

    private SoundConfig openSound;
    private SoundConfig purchaseSound;
    private SoundConfig forceRotateSound;
    private SoundConfig revealSound;

    public record PlaceholderItem(
            String material,
            String texture,
            String owner,
            String name,
            List<String> lore
    ) {}

    public record SoundConfig(
            String sound,
            float volume,
            float pitch
    ) {}

    public ConfigManager(ModernStore plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean load() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        boolean regenerated = false;
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            regenerated = true;
        }
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        prefix         = cfg.getString("prefix", "<gray>[<gold>ModernStore</gold>]</gray> ");
        rotationMillis = parseRotationString(cfg.getString("rotation", "1d 0h 0m"));
        currency       = cfg.getString("currency", "gold");
        defaultPool    = cfg.getString("default-pool", "default");

        broadcastEnabled  = cfg.getBoolean("broadcast.enabled", true);
        broadcastRotation = cfg.getBoolean("broadcast.rotation", true);
        broadcastWarnings = cfg.getBoolean("broadcast.warnings", true);

        hiddenItem  = readPlaceholderItem(cfg, "hidden-item",   "BARRIER", "<gray>Secret item</gray>");
        vipItem     = readPlaceholderItem(cfg, "vip-item",      "BARRIER", "<gold>VIP item</gold>");
        soldOutItem = readPlaceholderItem(cfg, "sold-out-item", "BARRIER", "<red>Sold out</red>");

        loadMessages(cfg);
        loadSounds(cfg);

        return regenerated;
    }

    private PlaceholderItem readPlaceholderItem(FileConfiguration cfg, String section,
                                                String defaultMat, String defaultName) {
        String mat     = cfg.getString(section + ".material", cfg.getString(section + "-material", defaultMat));
        String texture = cfg.getString(section + ".texture", "");
        String owner   = cfg.getString(section + ".owner", "");
        String name    = cfg.getString(section + ".name",  cfg.getString(section + "-name", defaultName));
        List<String> lore = cfg.getStringList(section + ".lore");
        return new PlaceholderItem(mat, texture, owner, name, lore);
    }

    private void loadMessages(FileConfiguration cfg) {
        messages.clear();
        warnings.clear();

        ConfigurationSection msgSec = cfg.getConfigurationSection("messages");
        if (msgSec == null) return;

        for (String key : msgSec.getKeys(false)) {
            if (key.equals("warnings")) continue;
            String value = msgSec.getString(key);
            if (value != null) messages.put(key, value);
        }

        ConfigurationSection warnSec = msgSec.getConfigurationSection("warnings");
        if (warnSec == null) return;

        for (String key : warnSec.getKeys(false)) {
            try {
                int minutes = Integer.parseInt(key);
                String msg  = warnSec.getString(key);
                if (msg != null) warnings.put(minutes, msg);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[ModernStore/Config] Non-numeric warning key '" + key + "' in config.yml — skipped.");
            }
        }
    }

    private void loadSounds(FileConfiguration cfg) {
        openSound       = readSound(cfg, "sounds.gui-open",    "BLOCK_CHEST_OPEN",             1f, 1f);
        purchaseSound   = readSound(cfg, "sounds.purchase",    "ENTITY_PLAYER_LEVELUP",         1f, 1f);
        forceRotateSound= readSound(cfg, "sounds.force-rotate","BLOCK_NOTE_BLOCK_PLING",        1f, 1f);
        revealSound     = readSound(cfg, "sounds.hidden-reveal","ENTITY_EXPERIENCE_ORB_PICKUP", 1f, 1.2f);
    }

    private SoundConfig readSound(FileConfiguration cfg, String path,
                                  String defaultSound, float defaultVol, float defaultPitch) {
        String sound = cfg.getString(path + ".sound", defaultSound);
        float  vol   = (float) cfg.getDouble(path + ".volume", defaultVol);
        float  pitch = (float) cfg.getDouble(path + ".pitch",  defaultPitch);
        return new SoundConfig(sound, vol, pitch);
    }

    private long parseRotationString(String input) {
        if (input == null || input.isBlank()) return 86_400_000L;

        long total = 0;
        for (String part : input.toLowerCase().split("\\s+")) {
            try {
                if (part.endsWith("d"))   total += Long.parseLong(part.replace("d", ""))   * 86_400_000L;
                else if (part.endsWith("h"))   total += Long.parseLong(part.replace("h", ""))   *  3_600_000L;
                else if (part.endsWith("min")) total += Long.parseLong(part.replace("min", "")) *     60_000L;
                else if (part.endsWith("m"))   total += Long.parseLong(part.replace("m", ""))   *     60_000L;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[ModernStore/Config] Malformed rotation token '" + part + "' — ignored.");
            }
        }
        return total > 0 ? total : 86_400_000L;
    }

    public String getPrefix() { return prefix; }

    public String getMessage(String key) {
        return prefix + messages.getOrDefault(key, "<red>Message not found: " + key + "</red>");
    }

    public String getRawMessage(String key) { return messages.getOrDefault(key, ""); }

    public long   getRotationMillis()   { return rotationMillis;  }
    public String getCurrency()         { return currency;        }
    public String getDefaultPool()      { return defaultPool;     }

    public boolean isBroadcastEnabled()  { return broadcastEnabled;  }
    public boolean isBroadcastRotation() { return broadcastRotation; }
    public boolean isBroadcastWarnings() { return broadcastWarnings; }

    public PlaceholderItem getHiddenItem()  { return hiddenItem;  }
    public PlaceholderItem getVipItem()     { return vipItem;     }
    public PlaceholderItem getSoldOutItem() { return soldOutItem; }

    public Map<Integer, String> getWarnings()       { return warnings;        }

    public SoundConfig getOpenSound()        { return openSound;        }
    public SoundConfig getPurchaseSound()    { return purchaseSound;    }
    public SoundConfig getForceRotateSound() { return forceRotateSound; }
    public SoundConfig getRevealSound()      { return revealSound;      }
}
