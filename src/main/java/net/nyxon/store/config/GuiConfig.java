//made with ❤ by nyxon.
package net.nyxon.store.config;

import net.nyxon.store.NyxonStore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiConfig {

    private final NyxonStore plugin;

    private String title;
    private int    size;
    private String lorePrice;
    private String lorePurchaseHint;
    private final Map<Integer, SlotConfig> slots = new HashMap<>();

    public record SlotConfig(
            String type,
            String material,
            String name,
            List<String> lore
    ) {}

    public GuiConfig(NyxonStore plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean load() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        boolean regenerated = false;

        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
            regenerated = true;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(guiFile);
        loadSettings(cfg);
        loadSlots(cfg);
        return regenerated;
    }

    private void loadSettings(FileConfiguration cfg) {
        title           = cfg.getString("title", "<b><color:#2B2B2B>nyxon store</color></b>");
        size            = cfg.getInt("size", 54);
        lorePrice       = cfg.getString("lore.price",         "<gold>Price: </gold><yellow>{price} {currency}</yellow>");
        lorePurchaseHint= cfg.getString("lore.purchase-hint", "<gray>Click to <green>purchase</green>!</gray>");
    }

    private void loadSlots(FileConfiguration cfg) {
        slots.clear();
        ConfigurationSection slotsSection = cfg.getConfigurationSection("slots");
        if (slotsSection == null) return;

        for (String key : slotsSection.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("[NyxonStore/GuiConfig] Non-numeric slot key '" + key + "' in gui.yml — skipped.");
                continue;
            }

            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning("[NyxonStore/GuiConfig] Slot " + slot
                        + " in gui.yml is out of range for inventory size " + size + " — skipped.");
                continue;
            }

            ConfigurationSection slotSec = slotsSection.getConfigurationSection(key);
            if (slotSec == null) continue;

            slots.put(slot, new SlotConfig(
                    slotSec.getString("type",     "filler"),
                    slotSec.getString("material", "BLACK_STAINED_GLASS_PANE"),
                    slotSec.getString("name",     " "),
                    slotSec.getStringList("lore")
            ));
        }
    }

    public String getTitle()            { return title;            }
    public int    getSize()             { return size;             }
    public String getLorePrice()        { return lorePrice;        }
    public String getLorePurchaseHint() { return lorePurchaseHint; }

    public Map<Integer, SlotConfig> getSlots() { return slots; }
}
