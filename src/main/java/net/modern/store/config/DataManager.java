package net.modern.store.config;

import net.modern.store.ModernStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    private final ModernStore plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    private final Map<String, Long>                          nextRotationTimes = new HashMap<>();
    private final Map<String, List<String>>                  currentItemsMap   = new HashMap<>();
    private final Map<String, Map<String, Integer>>          purchaseCounts    = new HashMap<>();
    private final Map<UUID, Map<String, Set<String>>>        revealedItems     = new HashMap<>();

    public DataManager(ModernStore plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        ensureFileExists();
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
    }

    private boolean ensureFileExists() {
        if (dataFile.exists()) return false;
        File dir = plugin.getDataFolder();
        if (!dir.exists()) dir.mkdirs();
        try {
            dataFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("[ModernStore/DataManager] Could not create data.yml: " + e.getMessage());
        }
        return true;
    }

    public boolean reload() {
        boolean regenerated = ensureFileExists();
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
        return regenerated;
    }

    public void loadData() {
        nextRotationTimes.clear();
        currentItemsMap.clear();
        purchaseCounts.clear();
        revealedItems.clear();

        loadRotations();
        loadPurchases();
        loadRevealedItems();
    }

    private void loadRotations() {
        ConfigurationSection sec = dataConfig.getConfigurationSection("rotations");
        if (sec == null) return;
        for (String pool : sec.getKeys(false)) {
            nextRotationTimes.put(pool, sec.getLong(pool + ".next-time", 0L));
            currentItemsMap.put(pool, sec.getStringList(pool + ".current-items"));
        }
    }

    private void loadPurchases() {
        ConfigurationSection sec = dataConfig.getConfigurationSection("purchases");
        if (sec == null) return;
        for (String pool : sec.getKeys(false)) {
            Map<String, Integer> poolPurchases = new HashMap<>();
            ConfigurationSection poolSec = sec.getConfigurationSection(pool);
            if (poolSec != null) {
                for (String itemId : poolSec.getKeys(false)) {
                    poolPurchases.put(itemId, poolSec.getInt(itemId, 0));
                }
            }
            purchaseCounts.put(pool, poolPurchases);
        }
    }

    private void loadRevealedItems() {
        ConfigurationSection sec = dataConfig.getConfigurationSection("revealed-items");
        if (sec == null) return;
        for (String uuidStr : sec.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[ModernStore/DataManager] Malformed UUID '" + uuidStr
                        + "' in data.yml revealed-items — skipped.");
                continue;
            }
            Map<String, Set<String>> playerRevealed = new HashMap<>();
            ConfigurationSection playerSec = sec.getConfigurationSection(uuidStr);
            if (playerSec != null) {
                for (String pool : playerSec.getKeys(false)) {
                    playerRevealed.put(pool, new HashSet<>(playerSec.getStringList(pool)));
                }
            }
            revealedItems.put(uuid, playerRevealed);
        }
    }

    public void saveData() {
        writeToConfig();
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[ModernStore/DataManager] Could not save data.yml: " + e.getMessage());
        }
    }

    public void saveDataAsync() {
        Map<String, Long>                       rotSnap  = new HashMap<>(nextRotationTimes);
        Map<String, List<String>>               itemsSnap= new HashMap<>(currentItemsMap);
        Map<String, Map<String, Integer>>        purchSnap= deepCopyPurchases();
        Map<UUID, Map<String, Set<String>>>      revSnap  = deepCopyRevealed();

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (DataManager.this) {
                applySnapshot(rotSnap, itemsSnap, purchSnap, revSnap);
                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("[ModernStore/DataManager] Async save failed: " + e.getMessage());
                }
            }
        });
    }

    private void writeToConfig() {
        dataConfig.set("rotations",     null);
        dataConfig.set("purchases",     null);
        dataConfig.set("revealed-items",null);

        for (Map.Entry<String, Long> e : nextRotationTimes.entrySet()) {
            String pool = e.getKey();
            dataConfig.set("rotations." + pool + ".next-time",     e.getValue());
            dataConfig.set("rotations." + pool + ".current-items", currentItemsMap.getOrDefault(pool, new ArrayList<>()));
        }
        for (Map.Entry<String, Map<String, Integer>> poolEntry : purchaseCounts.entrySet()) {
            String pool = poolEntry.getKey();
            for (Map.Entry<String, Integer> itemEntry : poolEntry.getValue().entrySet()) {
                dataConfig.set("purchases." + pool + "." + itemEntry.getKey(), itemEntry.getValue());
            }
        }
        for (Map.Entry<UUID, Map<String, Set<String>>> playerEntry : revealedItems.entrySet()) {
            String uuidStr = playerEntry.getKey().toString();
            for (Map.Entry<String, Set<String>> poolEntry : playerEntry.getValue().entrySet()) {
                dataConfig.set("revealed-items." + uuidStr + "." + poolEntry.getKey(),
                        new ArrayList<>(poolEntry.getValue()));
            }
        }
    }

    private synchronized void applySnapshot(Map<String, Long> rot,
                                             Map<String, List<String>> items,
                                             Map<String, Map<String, Integer>> purch,
                                             Map<UUID, Map<String, Set<String>>> rev) {
        dataConfig.set("rotations",     null);
        dataConfig.set("purchases",     null);
        dataConfig.set("revealed-items",null);

        for (Map.Entry<String, Long> e : rot.entrySet()) {
            String pool = e.getKey();
            dataConfig.set("rotations." + pool + ".next-time",     e.getValue());
            dataConfig.set("rotations." + pool + ".current-items", items.getOrDefault(pool, new ArrayList<>()));
        }
        for (Map.Entry<String, Map<String, Integer>> poolEntry : purch.entrySet()) {
            String pool = poolEntry.getKey();
            for (Map.Entry<String, Integer> itemEntry : poolEntry.getValue().entrySet()) {
                dataConfig.set("purchases." + pool + "." + itemEntry.getKey(), itemEntry.getValue());
            }
        }
        for (Map.Entry<UUID, Map<String, Set<String>>> playerEntry : rev.entrySet()) {
            String uuidStr = playerEntry.getKey().toString();
            for (Map.Entry<String, Set<String>> poolEntry : playerEntry.getValue().entrySet()) {
                dataConfig.set("revealed-items." + uuidStr + "." + poolEntry.getKey(),
                        new ArrayList<>(poolEntry.getValue()));
            }
        }
    }

    private Map<String, Map<String, Integer>> deepCopyPurchases() {
        Map<String, Map<String, Integer>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : purchaseCounts.entrySet()) {
            copy.put(e.getKey(), new HashMap<>(e.getValue()));
        }
        return copy;
    }

    private Map<UUID, Map<String, Set<String>>> deepCopyRevealed() {
        Map<UUID, Map<String, Set<String>>> copy = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Set<String>>> e : revealedItems.entrySet()) {
            Map<String, Set<String>> pools = new HashMap<>();
            for (Map.Entry<String, Set<String>> p : e.getValue().entrySet()) {
                pools.put(p.getKey(), new HashSet<>(p.getValue()));
            }
            copy.put(e.getKey(), pools);
        }
        return copy;
    }

    public long getNextRotationTime(String pool) { return nextRotationTimes.getOrDefault(pool, 0L); }
    public void setNextRotationTime(String pool, long time) { nextRotationTimes.put(pool, time); }

    public List<String> getCurrentItems(String pool)               { return currentItemsMap.getOrDefault(pool, new ArrayList<>()); }
    public void         setCurrentItems(String pool, List<String> ids) { currentItemsMap.put(pool, ids); }

    public int getPurchaseCount(String pool, String itemId) {
        return purchaseCounts.getOrDefault(pool, Collections.emptyMap()).getOrDefault(itemId, 0);
    }

    public void incrementPurchaseCount(String pool, String itemId) {
        purchaseCounts.computeIfAbsent(pool, k -> new HashMap<>()).merge(itemId, 1, Integer::sum);
    }

    public void resetPurchases(String pool) { purchaseCounts.remove(pool); }

    public boolean isItemRevealed(UUID playerUUID, String pool, String itemId) {
        Map<String, Set<String>> playerMap = revealedItems.get(playerUUID);
        if (playerMap == null) return false;
        Set<String> poolSet = playerMap.get(pool);
        return poolSet != null && poolSet.contains(itemId);
    }

    public void revealItem(UUID playerUUID, String pool, String itemId) {
        revealedItems
            .computeIfAbsent(playerUUID, k -> new HashMap<>())
            .computeIfAbsent(pool, k -> new HashSet<>())
            .add(itemId);
        saveDataAsync();
    }

    public void resetRevealed(String pool) {
        revealedItems.values().forEach(m -> m.remove(pool));
    }
}
