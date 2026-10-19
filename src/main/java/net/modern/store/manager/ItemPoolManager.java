package net.modern.store.manager;

import net.modern.store.ModernStore;
import net.modern.store.config.GuiConfig;
import net.modern.store.config.PoolConfig;
import net.modern.store.model.StoreItem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ItemPoolManager {

    private final ModernStore plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Map<String, List<StoreItem>> activeItemsMap  = new HashMap<>();
    private final Map<String, Set<Integer>>    sentWarningsMap = new HashMap<>();
    private final Map<String, Long> poolGenerations = new HashMap<>();

    public ItemPoolManager(ModernStore plugin) {
        this.plugin = plugin;
        loadAllActiveItems();
    }

    public void loadAllActiveItems() {
        for (String poolName : plugin.getPoolConfig().getPoolNames()) {
            loadActiveItems(poolName);
        }
    }

    public void loadActiveItems(String poolName) {
        List<String>    savedIds    = plugin.getDataManager().getCurrentItems(poolName);
        List<StoreItem> pool        = plugin.getPoolConfig().getPool(poolName);
        List<StoreItem> activeItems = new ArrayList<>();

        if (!savedIds.isEmpty()) {
            for (String id : savedIds) {
                pool.stream()
                    .filter(i -> i.id().equals(id))
                    .findFirst()
                    .ifPresent(activeItems::add);
            }
        }

        List<Integer> poolSlots = getPoolSlots(poolName);
        long savedNext = plugin.getDataManager().getNextRotationTime(poolName);
        boolean expired = savedNext > 0 && System.currentTimeMillis() >= savedNext;
        boolean wrongSize = activeItems.size() != poolSlots.size();

        if (wrongSize || expired) {
            rotatePool(poolName, false);
        } else {
            activeItemsMap.put(poolName, activeItems);
        }
    }

    public List<Integer> getPoolSlots(String poolName) {
        Map<Integer, GuiConfig.SlotConfig> slotConfig;
        PoolConfig.CustomGuiConfig custom = plugin.getPoolConfig().getCustomGui(poolName);
        if (custom != null && !custom.getSlots().isEmpty()) {
            slotConfig = custom.getSlots();
        } else {
            slotConfig = plugin.getGuiConfig().getSlots();
        }

        List<Integer> slots = new ArrayList<>();
        for (Map.Entry<Integer, GuiConfig.SlotConfig> entry : slotConfig.entrySet()) {
            if ("pool".equals(entry.getValue().type())) {
                slots.add(entry.getKey());
            }
        }
        if (slots.isEmpty()) {
            slots = Arrays.asList(21, 22, 23);
        }
        return slots;
    }

    public void rotatePool(String poolName, boolean force) {
        List<StoreItem> pool = plugin.getPoolConfig().getPool(poolName);
        if (pool.isEmpty()) {
            plugin.getLogger().warning("[ModernStore/PoolManager] Pool '" + poolName
                    + "' is empty — cannot rotate.");
            return;
        }

        List<Integer> poolSlots  = getPoolSlots(poolName);
        List<StoreItem> selected = weightedSampleWithoutReplacement(pool, poolSlots.size());

        activeItemsMap.put(poolName, selected);

        List<String> ids = selected.stream().map(StoreItem::id).toList();
        plugin.getDataManager().setCurrentItems(poolName, ids);
        plugin.getDataManager().setNextRotationTime(poolName,
                System.currentTimeMillis() + plugin.getConfigManager().getRotationMillis());
        plugin.getDataManager().resetPurchases(poolName);
        plugin.getDataManager().resetRevealed(poolName);
        plugin.getDataManager().saveDataAsync();

        poolGenerations.merge(poolName, 1L, Long::sum);
        sentWarningsMap.put(poolName, new HashSet<>());

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().buildCache(poolName);
            plugin.getGuiManager().refreshViewers(poolName);
        }

        if (plugin.getConfigManager().isBroadcastEnabled()
                && plugin.getConfigManager().isBroadcastRotation()) {
            Bukkit.broadcast(MM.deserialize(plugin.getConfigManager().getMessage("store-rotated")));
        }
    }

    private List<StoreItem> weightedSampleWithoutReplacement(List<StoreItem> pool, int count) {
        List<StoreItem> available = new ArrayList<>(pool);
        List<StoreItem> result    = new ArrayList<>(Math.min(count, available.size()));
        ThreadLocalRandom rng     = ThreadLocalRandom.current();

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            int totalWeight = available.stream().mapToInt(StoreItem::weight).sum();
            if (totalWeight <= 0) break;

            int value  = rng.nextInt(totalWeight);
            int cursor = 0;
            StoreItem chosen = null;

            for (StoreItem item : available) {
                cursor += item.weight();
                if (cursor > value) {
                    chosen = item;
                    break;
                }
            }
            if (chosen != null) {
                result.add(chosen);
                available.remove(chosen);
            }
        }
        return result;
    }

    public void setPoolItems(String poolName, List<String> itemIds) {
        List<StoreItem> pool        = plugin.getPoolConfig().getPool(poolName);
        List<StoreItem> activeItems = new ArrayList<>();
        for (String id : itemIds) {
            pool.stream()
                .filter(i -> i.id().equals(id))
                .findFirst()
                .ifPresent(activeItems::add);
        }

        activeItemsMap.put(poolName, activeItems);
        plugin.getDataManager().setCurrentItems(poolName, itemIds);
        plugin.getDataManager().resetPurchases(poolName);
        plugin.getDataManager().resetRevealed(poolName);
        plugin.getDataManager().saveDataAsync();
        poolGenerations.merge(poolName, 1L, Long::sum);

        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().buildCache(poolName);
            plugin.getGuiManager().refreshViewers(poolName);
        }
    }

    public void refreshActiveItems(String poolName) {
        List<StoreItem> current = activeItemsMap.get(poolName);
        if ((current == null || current.isEmpty())
                && plugin.getDataManager().getNextRotationTime(poolName) <= 0) {
            rotatePool(poolName, false);
            return;
        }

        List<StoreItem> pool      = plugin.getPoolConfig().getPool(poolName);
        List<StoreItem> refreshed = new ArrayList<>();
        if (current != null) {
            for (StoreItem active : current) {
                pool.stream()
                    .filter(i -> i.id().equals(active.id()))
                    .findFirst()
                    .ifPresent(refreshed::add);
            }
        }
        activeItemsMap.put(poolName, refreshed);
    }

    public void checkTimer() {
        long now = System.currentTimeMillis();

        for (String poolName : plugin.getPoolConfig().getPoolNames()) {
            long nextRotation = plugin.getDataManager().getNextRotationTime(poolName);
            if (nextRotation <= 0) continue;

            long diff = nextRotation - now;

            if (diff <= 0) {
                rotatePool(poolName, false);
                continue;
            }

            if (!plugin.getConfigManager().isBroadcastEnabled()
                    || !plugin.getConfigManager().isBroadcastWarnings()) continue;

            Set<Integer> sent     = sentWarningsMap.computeIfAbsent(poolName, k -> new HashSet<>());
            Map<Integer, String> warnings = plugin.getConfigManager().getWarnings();

            for (Map.Entry<Integer, String> entry : warnings.entrySet()) {
                int  minutes      = entry.getKey();
                long thresholdMs  = (long) minutes * 60_000L;
                if (!sent.contains(minutes) && diff <= thresholdMs && diff > thresholdMs - 2_000L) {
                    sent.add(minutes);
                    Bukkit.broadcast(MM.deserialize(
                            plugin.getConfigManager().getPrefix() + entry.getValue()));
                }
            }
        }
    }

    public List<StoreItem> getActiveItems(String poolName) {
        return activeItemsMap.getOrDefault(poolName, Collections.emptyList());
    }

    public long getPoolGeneration(String poolName) {
        return poolGenerations.getOrDefault(poolName, 0L);
    }
}
