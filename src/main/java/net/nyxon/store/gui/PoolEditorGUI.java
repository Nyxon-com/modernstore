//made with ❤ by nyxon
package net.nyxon.store.gui;

import net.nyxon.store.NyxonStore;
import net.nyxon.store.config.PoolConfig;
import net.nyxon.store.model.StoreItem;
import net.nyxon.store.util.MaterialHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class PoolEditorGUI implements Listener {

    private final NyxonStore plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int ITEMS_PER_PAGE = 45;

    private final Map<UUID, ChatInputState> pendingChatInputs = new HashMap<>();

    public enum MenuState { MAIN_MENU, POOL_EDITOR, ITEM_EDITOR }

    public static class ChatInputState {
        private final String poolName;
        private final String itemId;
        private final String action;
        ChatInputState(String poolName, String itemId, String action) {
            this.poolName = poolName; this.itemId = itemId; this.action = action;
        }
        public String getPoolName() { return poolName; }
        public String getItemId()   { return itemId;   }
        public String getAction()   { return action;   }
    }

    public PoolEditorGUI(NyxonStore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        EditorHolder holder = new EditorHolder(MenuState.MAIN_MENU, null, null, null, null);
        Inventory inv = Bukkit.createInventory(holder, 45,
                MM.deserialize("<gold>NyxonStore Editor <gray>| <white>Pool List"));
        fillEditor(inv, 45);

        setItem(inv, 41, Material.LIME_WOOL,
                "<green>Create New Pool", List.of("<gray>Click to create a new pool!"));
        setItem(inv, 39, Material.ARROW, "<yellow>Refresh", List.of());

        List<String> pools = plugin.getPoolConfig().getPoolNames();
        String defaultPool = plugin.getConfigManager().getDefaultPool();
        for (int i = 0; i < Math.min(36, pools.size()); i++) {
            String pn        = pools.get(i);
            boolean isDef    = pn.equalsIgnoreCase(defaultPool);
            ItemStack pi     = new ItemStack(isDef ? Material.ENDER_CHEST : Material.CHEST);
            ItemMeta  meta   = pi.getItemMeta();
            meta.displayName(MM.deserialize("<gold>" + pn + (isDef ? " <yellow>(Default)" : ""))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                MM.deserialize("<gray>Left-click: edit pool").decoration(TextDecoration.ITALIC, false),
                MM.deserialize("<gray>Right-click: set as default").decoration(TextDecoration.ITALIC, false),
                MM.deserialize("<gray>Shift-click: force rotation").decoration(TextDecoration.ITALIC, false)));
            pi.setItemMeta(meta);
            inv.setItem(i, pi);
        }
        player.openInventory(inv);
    }

    public void openPoolEditor(Player player, String poolName) { openPoolEditor(player, poolName, 0); }

    public void openPoolEditor(Player player, String poolName, int page) {
        List<StoreItem> items = new ArrayList<>(plugin.getPoolConfig().getPool(poolName));
        items.sort(Comparator.comparing(StoreItem::id, String.CASE_INSENSITIVE_ORDER));
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        EditorHolder holder = new EditorHolder(MenuState.POOL_EDITOR, poolName, null, null,
                String.valueOf(page));
        Inventory inv = Bukkit.createInventory(holder, 54,
                MM.deserialize("<gold>Editor <gray>| <white>" + poolName
                        + " <gray>(" + (page + 1) + "/" + totalPages + ")"));

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= items.size()) break;
            inv.setItem(i, buildDisplayItem(items.get(idx)));
        }
        fillEditor(inv, 45, 54);

        setItem(inv, 45, Material.RED_BED,    "<red>Back",      List.of());
        setItem(inv, 49, Material.LIME_WOOL,  "<green>Add Item", List.of("<gray>Click to add a new item"));
        if (page > 0)          setItem(inv, 48, Material.ARROW, "<yellow>Previous page", List.of());
        if (page < totalPages - 1) setItem(inv, 50, Material.ARROW, "<yellow>Next page",     List.of());
        player.openInventory(inv);
    }

    public void openItemEditor(Player player, String poolName, String itemId) {
        StoreItem item = plugin.getPoolConfig().getPool(poolName).stream()
                .filter(i -> i.id().equals(itemId)).findFirst().orElse(null);
        if (item == null) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Item not found!"));
            openPoolEditor(player, poolName);
            return;
        }
        EditorHolder holder = new EditorHolder(MenuState.ITEM_EDITOR, poolName, itemId, null, null);
        Inventory inv = Bukkit.createInventory(holder, 45,
                MM.deserialize("<gold>Editor <gray>| <white>Item: " + itemId));
        fillEditor(inv, 45);

        setItem(inv, 36, Material.RED_BED,      "<red>Back",        List.of());
        setItem(inv, 44, Material.REDSTONE_BLOCK,"<red>Delete Item", List.of());

        String idText;
        switch (item.type().toLowerCase()) {
            case "mmoitems"   -> idText = item.mmoItemType() + "/" + item.mmoItemId();
            case "itemsadder" -> idText = item.itemsadderId();
            case "command"    -> idText = (item.displayMaterial() != null ? item.displayMaterial() : "COMMAND_BLOCK")
                    + " | cmds=" + (item.commands() != null ? item.commands().size() : 0);
            default           -> idText = item.material();
        }
        String displayCur = (item.currency() == null || item.currency().isBlank())
                ? "<gray>(default: " + plugin.getConfigManager().getCurrency() + ")"
                : "<white>" + item.currency();

        setItem(inv, 10, Material.ITEM_FRAME,  "<blue>Type: <white>"   + item.type(),
                List.of("<gray>Click to cycle (vanilla / mmoitems / itemsadder / command)"));
        setItem(inv, 12, Material.PAPER,       "<blue>Item: <white>"   + idText,
                List.of("<gray>Click to change"));
        setItem(inv, 14, Material.COOKIE,      "<blue>Amount: <white>" + item.amount(),
                List.of("<gray>Click to change"));
        setItem(inv, 16, Material.GOLD_NUGGET, "<blue>Price: <white>"  + formatPrice(item.price()),
                List.of("<gray>Click to change"));
        setItem(inv, 22, Material.ANVIL,       "<blue>Slot: <white>"   + (item.slot() == -1 ? "Random" : item.slot()),
                List.of("<gray>Click to change via chat"));
        setItem(inv, 24, Material.NAME_TAG,    "<blue>Display name: <white>"
                + (item.displayName() == null || item.displayName().isBlank() ? "<gray>(default)" : item.displayName()),
                List.of("<gray>Click to change (MiniMessage)", "<gray>Type 'reset' to clear"));
        setItem(inv, 20, Material.WRITABLE_BOOK, "<blue>ID: <white>" + item.id(),
                List.of("<gray>Click to rename the internal ID",
                        "<gray>Note: resets this item's purchase counter"));
        setItem(inv, 28, Material.FEATHER,     "<blue>Weight: <white>"        + item.weight(),
                List.of("<gray>Click to change (higher = more likely)"));
        setItem(inv, 30, Material.CLOCK,       "<blue>Max purchases: <white>" + item.maxPurchases(),
                List.of("<gray>Click to change (-1 = unlimited)"));
        setItem(inv, 26, Material.GOLD_INGOT,  "<blue>Currency: " + displayCur,
                List.of("<gray>Click to change currency for this item",
                        "<gray>Type 'reset' to use default from config.yml"));
        if ("command".equalsIgnoreCase(item.type())) {
            setItem(inv, 2, Material.COMMAND_BLOCK, "<blue>Display Material: <white>"
                            + (item.displayMaterial() == null ? "COMMAND_BLOCK" : item.displayMaterial()),
                    List.of("<gray>Click to change the displayed item"));
            setItem(inv, 4, Material.CHAIN_COMMAND_BLOCK, "<blue>Commands: <white>"
                            + (item.commands() != null ? item.commands().size() : 0) + " commands",
                    List.of("<gray>Click to edit commands",
                            "<gray>Separate multiple commands with ;",
                            "<gray>Use {player} as placeholder for player name"));
        }

        inv.setItem(32, toggleItem(item.vip(),    "<blue>VIP: <white>"));
        inv.setItem(34, toggleItem(item.hidden(),  "<blue>Hidden: <white>"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top     = event.getView().getTopInventory();
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !clicked.equals(top)) return;
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof EditorHolder eh)) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        switch (eh.getState()) {
            case MAIN_MENU   -> handleMainMenu(player, slot, event);
            case POOL_EDITOR -> handlePoolEditor(player, slot, eh);
            case ITEM_EDITOR -> handleItemEditor(player, slot, eh, event);
        }
    }

    private void handleMainMenu(Player player, int slot, InventoryClickEvent event) {
        if (slot == 41) {
            startChat(player, null, null, "createpool",
                    "<green>Type the new pool name (letters, numbers, _ and -; 'cancel' to abort):</green>");
        } else if (slot == 39) {
            plugin.getPoolConfig().load();
            openMainMenu(player);
        } else if (slot < 36) {
            List<String> pools = plugin.getPoolConfig().getPoolNames();
            if (slot >= pools.size()) return;
            String pn = pools.get(slot);
            if (event.isLeftClick())  openPoolEditor(player, pn);
            else if (event.isRightClick()) {
                plugin.getConfig().set("default-pool", pn);
                plugin.saveConfig();
                plugin.getConfigManager().load();
                player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                        + "<green>Default pool set to: " + pn));
                openMainMenu(player);
            } else if (event.isShiftClick()) {
                plugin.getItemPoolManager().rotatePool(pn, true);
                player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                        + "<green>Forced rotation for pool: " + pn));
                openMainMenu(player);
            }
        }
    }

    private void handlePoolEditor(Player player, int slot, EditorHolder eh) {
        String poolName = eh.getPoolName();
        int page = parsePage(eh.getTempText());
        if (slot == 45) { openMainMenu(player); return; }
        if (slot == 49) { startChat(player, poolName, null, "createitem",
                "<green>Type the new item ID ('cancel' to abort):</green>"); return; }
        if (slot == 48) { openPoolEditor(player, poolName, page - 1); return; }
        if (slot == 50) { openPoolEditor(player, poolName, page + 1); return; }
        if (slot < 45) {
            ItemStack is = getTopInventory(player).getItem(slot);
            if (is == null || is.getItemMeta() == null) return;
            NamespacedKey key = new NamespacedKey(plugin, "item_id");
            String storedId = is.getItemMeta().getPersistentDataContainer()
                    .get(key, PersistentDataType.STRING);
            if (storedId != null) openItemEditor(player, poolName, storedId);
        }
    }

    private Inventory getTopInventory(Player player) {
        return player.getOpenInventory().getTopInventory();
    }

    private void handleItemEditor(Player player, int slot, EditorHolder eh, InventoryClickEvent event) {
        String poolName = eh.getPoolName();
        String itemId   = eh.getItemId();
        StoreItem item  = plugin.getPoolConfig().getPool(poolName).stream()
                .filter(i -> i.id().equals(itemId)).findFirst().orElse(null);
        if (item == null) { openPoolEditor(player, poolName); return; }

        switch (slot) {
            case 36 -> openPoolEditor(player, poolName);
            case 44 -> { saveDeleteItem(poolName, itemId); openPoolEditor(player, poolName); }
            case 10 -> {
                String next = switch (item.type().toLowerCase()) {
                    case "vanilla"   -> "mmoitems";
                    case "mmoitems"  -> "itemsadder";
                    case "itemsadder"-> "command";
                    default          -> "vanilla";
                };
                saveField(player, poolName, itemId, "type", next);
            }
            case 12 -> {
                String prompt;
                String t = item.type().toLowerCase();
                if ("itemsadder".equals(t)) {
                    prompt = "<green>Type the ItemsAdder ID (e.g. namespace:item_id):</green>";
                } else if ("mmoitems".equals(t)) {
                    prompt = "<green>Type the MMOItems ID as TYPE/ID (e.g. SWORD/CUTLASS):</green>";
                } else if ("command".equals(t)) {
                    prompt = "<green>Type the display material (e.g. DIAMOND or COMMAND_BLOCK):</green>";
                } else {
                    prompt = "<green>Type the vanilla material (e.g. DIAMOND):</green>";
                }
                startChat(player, poolName, itemId, "edit_item", prompt);
            }
            case 2 -> {
                if ("command".equalsIgnoreCase(item.type()))
                    startChat(player, poolName, itemId, "edit_displaymaterial",
                            "<green>Type the display material (e.g. COMMAND_BLOCK):</green>");
            }
            case 4 -> {
                if ("command".equalsIgnoreCase(item.type()))
                    startChat(player, poolName, itemId, "edit_commands",
                            "<green>Type the commands, separated by ; (use {player} for the player name):</green>");
            }
            case 14 -> startChat(player, poolName, itemId, "edit_amount",
                    "<green>Type the amount (1-64):</green>");
            case 16 -> startChat(player, poolName, itemId, "edit_price",
                    "<green>Type the price (e.g. 250 or 99.5):</green>");
            case 28 -> startChat(player, poolName, itemId, "edit_weight",
                    "<green>Type the weight (e.g. 100):</green>");
            case 30 -> startChat(player, poolName, itemId, "edit_maxpurchases",
                    "<green>Type max purchases (-1 = unlimited):</green>");
            case 24 -> startChat(player, poolName, itemId, "edit_displayname",
                    "<green>Type display name (MiniMessage; 'reset' to clear):</green>");
            case 20 -> startChat(player, poolName, itemId, "renameid",
                    "<green>Type the new internal ID (letters, numbers, _ and -; 'cancel' to abort):</green>");
            case 26 -> startChat(player, poolName, itemId, "edit_currency",
                    "<green>Type currency name (e.g. gems / tokens); 'reset' to use default:</green>");
            case 22 -> {
                int guiSize = getGuiSize(poolName);
                startChat(player, poolName, itemId, "slot",
                        "<green>Type a slot number (0 to " + (guiSize - 1) + ", or -1 for random):</green>");
            }
            case 32 -> { saveToggleField(poolName, itemId, "vip");    openItemEditor(player, poolName, itemId); }
            case 34 -> { saveToggleField(poolName, itemId, "hidden");  openItemEditor(player, poolName, itemId); }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatInputState state = pendingChatInputs.get(player.getUniqueId());
        if (state == null) return;
        event.setCancelled(true);
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            pendingChatInputs.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> reopenFromState(player, state));
            return;
        }
        pendingChatInputs.remove(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> handleChatInput(player, state, msg));
    }

    private void handleChatInput(Player player, ChatInputState state, String input) {
        String poolName = state.getPoolName();
        String itemId   = state.getItemId();
        String action   = state.getAction();

        switch (action) {
            case "createpool" -> {
                if (!input.matches("[a-zA-Z0-9_-]+")) {
                    player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                            + "<red>Invalid pool name! Use only letters, numbers, _ and -."));
                    openMainMenu(player); return;
                }
                if (plugin.getPoolConfig().getPoolNames().contains(input)) {
                    player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                            + "<red>A pool with that name already exists!"));
                    openMainMenu(player); return;
                }
                saveCreatePool(input);
                player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                        + "<green>Created pool: " + input));
                openMainMenu(player);
            }
            case "createitem" -> {
                if (!input.matches("[a-zA-Z0-9_-]+")) {
                    player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                            + "<red>Invalid item ID! Use only letters, numbers, _ and -."));
                    openPoolEditor(player, poolName); return;
                }
                if (saveCreateItem(poolName, input)) {
                    player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                            + "<green>Created item: " + input));
                    openItemEditor(player, poolName, input);
                } else {
                    player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                            + "<red>An item with that ID already exists!"));
                    openPoolEditor(player, poolName);
                }
            }
            case "renameid" -> renameItemId(player, poolName, itemId, input);
            case "slot"     -> handleSlotInput(player, poolName, itemId, input, state);
            default -> {
                if (action.startsWith("edit_")) {
                    saveField(player, poolName, itemId, action.substring(5), input);
                }
            }
        }
    }

    private void handleSlotInput(Player player, String poolName, String itemId,
                                  String input, ChatInputState state) {
        int guiSize = getGuiSize(poolName);
        try {
            int slotNum = Integer.parseInt(input);
            if (slotNum < -1 || slotNum >= guiSize) {
                player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                        + "<red>Slot must be between -1 and " + (guiSize - 1) + "!"));
                pendingChatInputs.put(player.getUniqueId(), state);
                player.sendMessage(MM.deserialize("<green>Type slot number (0–"
                        + (guiSize - 1) + " or -1 for random):</green>"));
                return;
            }
            File file = poolFile(poolName);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            cfg.set("items." + itemId + ".slot", slotNum);
            cfg.save(file);
            reloadAndRefresh(poolName);
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<green>Slot set to: " + slotNum));
            openItemEditor(player, poolName, itemId);
        } catch (NumberFormatException e) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Invalid number!"));
            pendingChatInputs.put(player.getUniqueId(), state);
            player.sendMessage(MM.deserialize("<green>Type slot number (0–"
                    + (guiSize - 1) + " or -1 for random):</green>"));
        } catch (IOException e) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Save error: " + e.getMessage()));
            openItemEditor(player, poolName, itemId);
        }
    }

    private void renameItemId(Player player, String poolName, String oldId, String newId) {
        if (!newId.matches("[a-zA-Z0-9_-]+")) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Invalid ID!"));
            openItemEditor(player, poolName, oldId); return;
        }
        if (newId.equals(oldId)) { openItemEditor(player, poolName, oldId); return; }
        File file = poolFile(poolName);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        org.bukkit.configuration.ConfigurationSection oldSec =
                cfg.getConfigurationSection("items." + oldId);
        if (oldSec == null) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Original item not found!"));
            openPoolEditor(player, poolName); return;
        }
        if (cfg.contains("items." + newId)) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>An item with ID '" + newId + "' already exists!"));
            openItemEditor(player, poolName, oldId); return;
        }
        for (Map.Entry<String, Object> entry : oldSec.getValues(false).entrySet())
            cfg.set("items." + newId + "." + entry.getKey(), entry.getValue());
        cfg.set("items." + oldId, null);
        try {
            cfg.save(file);
            reloadAndRefresh(poolName);
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<green>Renamed: " + oldId + " → " + newId));
            openItemEditor(player, poolName, newId);
        } catch (IOException e) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Save error: " + e.getMessage()));
            openItemEditor(player, poolName, oldId);
        }
    }

    private void startChat(Player player, String poolName, String itemId,
                            String action, String prompt) {
        player.closeInventory();
        pendingChatInputs.put(player.getUniqueId(), new ChatInputState(poolName, itemId, action));
        player.sendMessage(MM.deserialize(prompt));
    }

    private void reopenFromState(Player player, ChatInputState state) {
        if (state.getPoolName() == null)  { openMainMenu(player); return; }
        if (state.getItemId()   != null)  { openItemEditor(player, state.getPoolName(), state.getItemId()); return; }
        openPoolEditor(player, state.getPoolName());
    }

    private void saveField(Player player, String poolName, String itemId, String field, String value) {
        File file = poolFile(poolName);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String path = "items." + itemId;
        try {
            switch (field) {
                case "type" -> {
                    cfg.set(path + ".type", value);
                    switch (value.toLowerCase()) {
                        case "itemsadder" -> {
                            cfg.set(path + ".material", null);
                            cfg.set(path + ".display-material", null);
                            cfg.set(path + ".commands", null);
                            nullifyMmoFields(cfg, path);
                            cfg.set(path + ".itemsadder-id", "");
                        }
                        case "mmoitems" -> {
                            cfg.set(path + ".material", null);
                            cfg.set(path + ".display-material", null);
                            cfg.set(path + ".commands", null);
                            cfg.set(path + ".mmoitems-id",   "");
                            cfg.set(path + ".mmoitems-type", "SWORD");
                            nullifyIaFields(cfg, path);
                        }
                        case "command" -> {
                            cfg.set(path + ".material", null);
                            nullifyMmoFields(cfg, path);
                            nullifyIaFields(cfg, path);
                            cfg.set(path + ".display-material", "COMMAND_BLOCK");
                            List<String> defCmds = cfg.getStringList(path + ".commands");
                            if (defCmds == null || defCmds.isEmpty())
                                cfg.set(path + ".commands", List.of("say Hello {player}!"));
                        }
                        default -> {
                            cfg.set(path + ".material", "STONE");
                            cfg.set(path + ".display-material", null);
                            cfg.set(path + ".commands", null);
                            nullifyMmoFields(cfg, path);
                            nullifyIaFields(cfg, path);
                        }
                    }
                }
                case "item" -> {
                    String type = cfg.getString(path + ".type", "vanilla");
                    if ("itemsadder".equalsIgnoreCase(type)) {
                        cfg.set(path + ".itemsadder-id", value);
                    } else if ("mmoitems".equalsIgnoreCase(type)) {
                        String[] sp = value.split("/", 2);
                        if (sp.length == 2) {
                            cfg.set(path + ".mmoitems-type", sp[0]);
                            cfg.set(path + ".mmoitems-id",   sp[1]);
                        } else {
                            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                                    + "<red>Use format TYPE/ID (e.g. SWORD/CUTLASS)."));
                            openItemEditor(player, poolName, itemId); return;
                        }
                    } else if ("command".equalsIgnoreCase(type)) {
                        cfg.set(path + ".display-material", value.toUpperCase());
                    } else {
                        cfg.set(path + ".material", value.toUpperCase());
                    }
                }
                case "displaymaterial" -> cfg.set(path + ".display-material", value.toUpperCase());
                case "commands" -> {
                    List<String> cmds = new ArrayList<>();
                    for (String part : value.split(";")) {
                        String c = part.trim();
                        if (!c.isEmpty()) cmds.add(c);
                    }
                    cfg.set(path + ".commands", cmds);
                }
                case "amount"       -> cfg.set(path + ".amount",        Integer.parseInt(value));
                case "price"        -> cfg.set(path + ".price",         Double.parseDouble(value));
                case "weight"       -> cfg.set(path + ".weight",        Integer.parseInt(value));
                case "maxpurchases" -> cfg.set(path + ".max-purchases", Integer.parseInt(value));
                case "currency"     -> {
                    if (value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("clear")
                            || value.equalsIgnoreCase("none") || value.equalsIgnoreCase("default")) {
                        cfg.set(path + ".currency", null);
                    } else {
                        cfg.set(path + ".currency", value);
                    }
                }
                case "displayname"  -> {
                    if (value.equalsIgnoreCase("reset") || value.equalsIgnoreCase("clear")
                            || value.equalsIgnoreCase("none")) {
                        cfg.set(path + ".display-name", null);
                    } else {
                        cfg.set(path + ".display-name", value);
                    }
                }
            }
            cfg.save(file);
            reloadAndRefresh(poolName);
        } catch (NumberFormatException e) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Invalid value: '" + value + "'"));
        } catch (IOException e) {
            player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
                    + "<red>Save error: " + e.getMessage()));
        }
        openItemEditor(player, poolName, itemId);
    }

    private void saveToggleField(String poolName, String itemId, String field) {
        File file = poolFile(poolName);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String fpath = "items." + itemId + "." + field;
        cfg.set(fpath, !cfg.getBoolean(fpath, false));
        trySave(cfg, file, "[NyxonStore/Editor] Cannot save toggle for " + poolName + "/" + itemId);
        reloadAndRefresh(poolName);
    }

    private void saveDeleteItem(String poolName, String itemId) {
        File file = poolFile(poolName);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("items." + itemId, null);
        trySave(cfg, file, "[NyxonStore/Editor] Cannot delete item " + itemId + " from " + poolName);
        reloadAndRefresh(poolName);
    }

    private boolean saveCreateItem(String poolName, String itemId) {
        File file = poolFile(poolName);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String path = "items." + itemId;
        if (cfg.contains(path)) return false;
        cfg.set(path + ".type",          "vanilla");
        cfg.set(path + ".material",      "STONE");
        cfg.set(path + ".amount",        1);
        cfg.set(path + ".price",         10);
        cfg.set(path + ".weight",        100);
        cfg.set(path + ".vip",           false);
        cfg.set(path + ".hidden",        false);
        cfg.set(path + ".max-purchases", -1);
        cfg.set(path + ".slot",          -1);
        trySave(cfg, file, "[NyxonStore/Editor] Cannot create item " + itemId + " in " + poolName);
        reloadAndRefresh(poolName);
        return true;
    }

    private void saveCreatePool(String name) {
        File file = poolFile(name);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String path = "items.example_item";
        cfg.set(path + ".type",          "vanilla");
        cfg.set(path + ".material",      "STONE");
        cfg.set(path + ".amount",        1);
        cfg.set(path + ".price",         10);
        cfg.set(path + ".weight",        100);
        cfg.set(path + ".vip",           false);
        cfg.set(path + ".hidden",        false);
        cfg.set(path + ".max-purchases", -1);
        cfg.set(path + ".slot",          -1);
        trySave(cfg, file, "[NyxonStore/Editor] Cannot create pool " + name);
        reloadAndRefresh(name);
    }

    private void nullifyMmoFields(FileConfiguration cfg, String path) {
        cfg.set(path + ".mmoitems-id",   null);
        cfg.set(path + ".mmoitems-type", null);
        cfg.set(path + ".mmoitems_id",   null);
        cfg.set(path + ".mmoitems_type", null);
    }

    private void nullifyIaFields(FileConfiguration cfg, String path) {
        cfg.set(path + ".itemsadder-id", null);
        cfg.set(path + ".itemsadder_id", null);
    }

    private void trySave(FileConfiguration cfg, File file, String errorMsg) {
        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().severe(errorMsg + ": " + e.getMessage()); }
    }

    private void reloadAndRefresh(String poolName) {
        plugin.getPoolConfig().load();
        plugin.getItemPoolManager().refreshActiveItems(poolName);
        plugin.getGuiManager().buildCache(poolName);
        plugin.getGuiManager().refreshViewers(poolName);
    }

    private ItemStack buildDisplayItem(StoreItem item) {
        ItemStack stack = plugin.getGuiManager().resolveItemStack(item);
        if (stack == null) stack = new ItemStack(Material.BARRIER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(plugin, "item_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, item.id());
            meta.displayName(MM.deserialize("<gold>" + item.id()).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            String displayCur = (item.currency() == null || item.currency().isBlank())
                    ? plugin.getConfigManager().getCurrency() : item.currency();
            lore.add(mm("<gray>Price: <yellow>" + formatPrice(item.price()) + " " + displayCur));
            lore.add(mm("<gray>Amount: <yellow>" + item.amount()));
            lore.add(mm("<gray>Weight: <yellow>" + item.weight()));
            lore.add(mm("<gray>Slot: <yellow>" + (item.slot() == -1 ? "Random" : item.slot())));
            lore.add(Component.empty());
            lore.add(mm("<gray>VIP: "    + (item.vip()    ? "<green>Yes" : "<red>No")));
            lore.add(mm("<gray>Hidden: " + (item.hidden() ? "<green>Yes" : "<red>No")));
            if ("command".equalsIgnoreCase(item.type())) {
                lore.add(Component.empty());
                lore.add(mm("<gray>Commands: <yellow>"
                        + (item.commands() != null ? item.commands().size() : 0)));
            }
            lore.add(Component.empty());
            lore.add(mm("<gray>Click to edit"));
            meta.lore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void setItem(Inventory inv, int slot, Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            List<Component> loreComp = new ArrayList<>();
            for (String l : lore) loreComp.add(MM.deserialize(l).decoration(TextDecoration.ITALIC, false));
            meta.lore(loreComp);
        }
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private ItemStack toggleItem(boolean value, String label) {
        ItemStack item = new ItemStack(value ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(label + (value ? "<green>Yes" : "<red>No"))
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillEditor(Inventory inv, int from, int to) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta  meta   = filler.getItemMeta();
        if (meta != null) { meta.displayName(Component.empty()); filler.setItemMeta(meta); }
        for (int i = from; i < to; i++) inv.setItem(i, filler);
    }

    private void fillEditor(Inventory inv, int size) { fillEditor(inv, 0, size); }

    private Component mm(String s) {
        return MM.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    private int parsePage(String v) {
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return 0; }
    }

    private int getGuiSize(String poolName) {
        PoolConfig.CustomGuiConfig c = plugin.getPoolConfig().getCustomGui(poolName);
        return c != null ? c.getSize() : plugin.getGuiConfig().getSize();
    }

    private File poolFile(String poolName) {
        return new File(plugin.getDataFolder(), "pools/" + poolName + ".yml");
    }

    private String formatPrice(double price) {
        BigDecimal bd = BigDecimal.valueOf(price)
                .setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        if (bd.scale() < 0) bd = bd.setScale(0);
        return bd.toPlainString();
    }
}
