package net.modern.store.config;

import net.modern.store.ModernStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfirmationConfig {

    private final ModernStore plugin;

    private boolean enabled;
    private String  title;
    private int     size;

    private int        confirmSlot;
    private SlotItem   confirmItem;
    private int        cancelSlot;
    private SlotItem   cancelItem;
    private int        infoSlot;
    private SlotItem   fillerItem;

    private final Map<Integer, SlotItem> decoSlots = new HashMap<>();

    public record SlotItem(
            String material,
            String texture,
            String owner,
            String name,
            List<String> lore
    ) {}

    public ConfirmationConfig(ModernStore plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean load() {
        File file = new File(plugin.getDataFolder(), "confirmation.yml");
        boolean regenerated = false;

        if (!file.exists()) {
            plugin.saveResource("confirmation.yml", false);
            regenerated = true;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        loadSettings(cfg);
        return regenerated;
    }

    private void loadSettings(FileConfiguration cfg) {
        decoSlots.clear();

        enabled = cfg.getBoolean("enabled", true);
        title   = cfg.getString("title", "<gold>Confirm Purchase</gold>");
        size    = cfg.getInt("size", 27);

        confirmSlot = cfg.getInt("confirm.slot", 11);
        confirmItem = readSlotItem(cfg.getConfigurationSection("confirm"), "LIME_WOOL",  "<green>Confirm</green>");
        cancelSlot  = cfg.getInt("cancel.slot", 15);
        cancelItem  = readSlotItem(cfg.getConfigurationSection("cancel"),  "RED_WOOL",   "<red>Cancel</red>");
        infoSlot    = cfg.getInt("info.slot", 13);
        fillerItem  = readSlotItem(cfg.getConfigurationSection("filler"),  "GRAY_STAINED_GLASS_PANE", " ");

        validateSlot("confirm", confirmSlot);
        validateSlot("cancel",  cancelSlot);
        validateSlot("info",    infoSlot);

        ConfigurationSection slotsSec = cfg.getConfigurationSection("slots");
        if (slotsSec != null) {
            for (String key : slotsSec.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[ModernStore/ConfirmationConfig] Non-numeric slot key '" + key
                            + "' in confirmation.yml — skipped.");
                    continue;
                }
                if (slot < 0 || slot >= size) {
                    plugin.getLogger().warning("[ModernStore/ConfirmationConfig] Decoration slot " + slot
                            + " out of range (0–" + (size - 1) + ") in confirmation.yml — skipped.");
                    continue;
                }
                decoSlots.put(slot, readSlotItem(slotsSec.getConfigurationSection(key),
                        "BLACK_STAINED_GLASS_PANE", " "));
            }
        }
    }

    private SlotItem readSlotItem(ConfigurationSection sec,
                                  String defaultMaterial, String defaultName) {
        if (sec == null) {
            return new SlotItem(defaultMaterial, "", "", defaultName, new ArrayList<>());
        }
        return new SlotItem(
                sec.getString("material", defaultMaterial),
                sec.getString("texture",  ""),
                sec.getString("owner",    ""),
                sec.getString("name",     defaultName),
                sec.getStringList("lore")
        );
    }

    private void validateSlot(String label, int slot) {
        if (slot < 0 || slot >= size) {
            plugin.getLogger().warning("[ModernStore/ConfirmationConfig] '" + label + "' slot " + slot
                    + " is out of range (0–" + (size - 1) + ") in confirmation.yml.");
        }
    }

    public boolean isEnabled()   { return enabled; }
    public String  getTitle()    { return title;   }
    public int     getSize()     { return size;    }

    public int      getConfirmSlot() { return confirmSlot; }
    public SlotItem getConfirmItem() { return confirmItem; }
    public int      getCancelSlot()  { return cancelSlot;  }
    public SlotItem getCancelItem()  { return cancelItem;  }
    public int      getInfoSlot()    { return infoSlot;    }
    public SlotItem getFillerItem()  { return fillerItem;  }

    public Map<Integer, SlotItem> getDecoSlots() { return decoSlots; }
}
