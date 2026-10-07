//made with ❤ by nyxon
package net.nyxon.store.config;

import net.nyxon.store.NyxonStore;
import net.nyxon.store.model.StoreItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class PoolConfig {

    private final NyxonStore plugin;
    private File poolsFolder;

    private final Map<String, List<StoreItem>> pools    = new HashMap<>();
    private final Map<String, CustomGuiConfig> poolGuis = new HashMap<>();

    public static class CustomGuiConfig {
        private final String title;
        private final int    size;
        private final Map<Integer, GuiConfig.SlotConfig> slots;

        public CustomGuiConfig(String title, int size, Map<Integer, GuiConfig.SlotConfig> slots) {
            this.title = title;
            this.size  = size;
            this.slots = slots;
        }

        public String getTitle() { return title; }
        public int    getSize()  { return size;  }
        public Map<Integer, GuiConfig.SlotConfig> getSlots() { return slots; }
    }

    public PoolConfig(NyxonStore plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean load() {
        poolsFolder = new File(plugin.getDataFolder(), "pools");
        boolean regenerated = false;

        if (!poolsFolder.exists()) {
            poolsFolder.mkdirs();
            regenerated = true;
        }

        File defaultPool = new File(poolsFolder, "default.yml");
        if (!defaultPool.exists()) {
            plugin.saveResource("pools/default.yml", false);
            regenerated = true;
        }

        pools.clear();
        poolGuis.clear();
        loadAllPools();
        return regenerated;
    }

    private void loadAllPools() {
        File[] files = poolsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String poolName = file.getName().replace(".yml", "");
            try {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                pools.put(poolName, loadPoolItems(cfg, poolName));
                loadCustomGui(cfg, poolName);
            } catch (Exception e) {
                plugin.getLogger().severe("[NyxonStore/PoolConfig] Failed to load pool file '"
                        + file.getName() + "': " + e.getMessage());
            }
        }
    }

    private List<StoreItem> loadPoolItems(FileConfiguration cfg, String poolName) {
        List<StoreItem> pool = new ArrayList<>();
        ConfigurationSection itemsSec = cfg.getConfigurationSection("items");
        if (itemsSec == null) {
            plugin.getLogger().warning("[NyxonStore/PoolConfig] Pool '" + poolName
                    + "' has no 'items:' section — pool will be empty.");
            return pool;
        }

        for (String key : itemsSec.getKeys(false)) {
            ConfigurationSection sec = itemsSec.getConfigurationSection(key);
            if (sec == null) {
                plugin.getLogger().warning("[NyxonStore/PoolConfig] Item '" + key
                        + "' in pool '" + poolName + "' is not a valid YAML section — skipped.");
                continue;
            }

            StoreItem item = parseItem(sec, key, poolName);
            if (item != null) pool.add(item);
        }
        return pool;
    }

    private StoreItem parseItem(ConfigurationSection sec, String key, String poolName) {
        String type       = sec.getString("type");
        String mmoType    = firstNonNull(sec.getString("mmoitems_type"), sec.getString("mmoitems-type"));
        String mmoId      = firstNonNull(sec.getString("mmoitems_id"),   sec.getString("mmoitems-id"));
        String material   = sec.getString("material");
        String iaId       = firstNonNull(sec.getString("itemsadder_id"), sec.getString("itemsadder-id"));
        String currency   = sec.getString("currency");
        String displayMat = firstNonNull(sec.getString("display-material"), sec.getString("display_material"));
        List<String> commands = sec.getStringList("commands");

        if (type == null || type.isBlank()) {
            if (mmoType != null && mmoId != null) type = "mmoitems";
            else if (iaId != null)                 type = "itemsadder";
            else if (commands != null && !commands.isEmpty()) type = "command";
            else                                   type = "vanilla";
        }

        switch (type.toLowerCase()) {
            case "vanilla" -> {
                if (material == null || material.isBlank()) {
                    skip(poolName, key, "type=vanilla but 'material' is missing");
                    return null;
                }
            }
            case "mmoitems" -> {
                if (mmoType == null || mmoType.isBlank() || mmoId == null || mmoId.isBlank()) {
                    skip(poolName, key, "type=mmoitems but 'mmoitems_type' or 'mmoitems_id' is missing");
                    return null;
                }
            }
            case "itemsadder" -> {
                if (iaId == null || iaId.isBlank()) {
                    skip(poolName, key, "type=itemsadder but 'itemsadder_id' is missing");
                    return null;
                }
            }
            case "command" -> {
                if (commands == null || commands.isEmpty()) {
                    skip(poolName, key, "type=command but 'commands' list is missing or empty");
                    return null;
                }
                if (displayMat == null || displayMat.isBlank()) displayMat = "COMMAND_BLOCK";
            }
            default -> {
                skip(poolName, key, "unknown type '" + type + "'");
                return null;
            }
        }

        int    amount      = positiveInt(sec, "amount",      1,   poolName, key);
        double price       = nonNegDouble(sec, "price",      0.0, poolName, key);
        int    weight      = positiveInt(sec, "weight",      10,  poolName, key);
        int    maxPurchases= sec.contains("max-purchases")
                             ? sec.getInt("max-purchases")
                             : sec.getInt("max_purchases", -1);
        int    slot        = sec.getInt("slot", -1);

        boolean vip    = sec.getBoolean("vip",    false);
        boolean hidden = sec.getBoolean("hidden", false);

        String displayName = firstNonNull(sec.getString("display-name"), sec.getString("display_name"));
        boolean consoleCommands;
        if (sec.contains("console-commands")) {
            consoleCommands = sec.getBoolean("console-commands", true);
        } else if (sec.contains("console_commands")) {
            consoleCommands = sec.getBoolean("console_commands", true);
        } else {
            consoleCommands = true;
        }

        return new StoreItem(key, type, mmoType, mmoId, material, iaId,
                amount, price, weight, vip, hidden, maxPurchases, slot, displayName,
                currency, displayMat, commands, consoleCommands);
    }

    private void loadCustomGui(FileConfiguration cfg, String poolName) {
        ConfigurationSection guiSec = cfg.getConfigurationSection("gui");
        if (guiSec == null) return;

        String title = guiSec.getString("title", "Store");
        int    size  = guiSec.getInt("size", 27);
        Map<Integer, GuiConfig.SlotConfig> slots = new HashMap<>();

        ConfigurationSection slotsSec = guiSec.getConfigurationSection("slots");
        if (slotsSec != null) {
            for (String key : slotsSec.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[NyxonStore/PoolConfig] Non-numeric slot key '"
                            + key + "' in pool '" + poolName + "' gui override — skipped.");
                    continue;
                }
                ConfigurationSection slotSec = slotsSec.getConfigurationSection(key);
                if (slotSec == null) continue;
                slots.put(slot, new GuiConfig.SlotConfig(
                        slotSec.getString("type",     "filler"),
                        slotSec.getString("material", "BLACK_STAINED_GLASS_PANE"),
                        slotSec.getString("name",     " "),
                        slotSec.getStringList("lore")
                ));
            }
        }

        poolGuis.put(poolName, new CustomGuiConfig(title, size, slots));
    }

    private void skip(String pool, String itemKey, String reason) {
        plugin.getLogger().warning("[NyxonStore/PoolConfig] Skipping item '" + itemKey
                + "' in pool '" + pool + "': " + reason + ".");
    }

    private int positiveInt(ConfigurationSection sec, String path, int def, String pool, String key) {
        int v = sec.getInt(path, def);
        if (v <= 0) {
            plugin.getLogger().warning("[NyxonStore/PoolConfig] Item '" + key + "' pool '" + pool
                    + "': '" + path + "' must be > 0 (got " + v + ") — using default " + def + ".");
            return def;
        }
        return v;
    }

    private double nonNegDouble(ConfigurationSection sec, String path, double def, String pool, String key) {
        double v = sec.getDouble(path, def);
        if (v < 0) {
            plugin.getLogger().warning("[NyxonStore/PoolConfig] Item '" + key + "' pool '" + pool
                    + "': '" + path + "' must be ≥ 0 (got " + v + ") — using default " + def + ".");
            return def;
        }
        return v;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    public List<StoreItem>           getPool(String name)     { return pools.getOrDefault(name, new ArrayList<>()); }
    public List<String>              getPoolNames()           { return new ArrayList<>(pools.keySet()); }
    public Map<String, List<StoreItem>> getAllPools()         { return new HashMap<>(pools); }
    public CustomGuiConfig           getCustomGui(String name){ return poolGuis.get(name); }
}
