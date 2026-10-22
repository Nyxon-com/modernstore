//made with love by nyxon

package net.modern.store.gui;

import net.modern.store.ModernStore;
import net.modern.store.config.ConfirmationConfig;
import net.modern.store.config.ConfigManager.PlaceholderItem;
import net.modern.store.config.ConfigManager.SoundConfig;
import net.modern.store.config.GuiConfig;
import net.modern.store.config.PoolConfig;
import net.modern.store.model.StoreItem;
import net.modern.store.util.HeadHelper;
import net.modern.store.util.MaterialHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ModernStoreGUI implements Listener {

  private final ModernStore plugin;
  private static final MiniMessage MM = MiniMessage.miniMessage();

  private final Map<String, List<ItemStack>> cachedItemsMap = new HashMap<>();

  public ModernStoreGUI(ModernStore plugin) {
    this.plugin = plugin;
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void buildCache(String poolName) {
    List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);
    List<ItemStack>  cached     = new ArrayList<>();

    for (StoreItem sItem : activeItems) {
      ItemStack item = resolveItemStack(sItem);
      if (item == null) {
        plugin.getLogger().warning("[ModernStore/GUI] Could not resolve ItemStack for item '"
            + sItem.id() + "' (type=" + sItem.type() + ") in pool '" + poolName
            + "' — slot will be empty.");
        cached.add(null);
        continue;
      }
      String currency = sItem.currency() != null && !sItem.currency().isBlank()
          ? sItem.currency() : plugin.getConfigManager().getCurrency();
      applyStoreLore(item, sItem, currency);
      cached.add(item);
    }
    cachedItemsMap.put(poolName, cached);
  }

  public void invalidateFiller() { cachedItemsMap.clear(); }

  public void open(Player player) {
    open(player, plugin.getConfigManager().getDefaultPool());
  }

  public void open(Player player, String poolName) {
    openInternal(player, poolName, true);
  }

  private void openSilent(Player player, String poolName) {
    openInternal(player, poolName, false);
  }

  private void openInternal(Player player, String poolName, boolean playSound) {
    if (!cachedItemsMap.containsKey(poolName)) buildCache(poolName);

    StoreHolder holder = new StoreHolder(poolName,
        plugin.getItemPoolManager().getPoolGeneration(poolName));
    Inventory inv = Bukkit.createInventory(holder,
        getGuiSize(poolName), getGuiTitle(poolName));

    fillBackground(inv, poolName);
    placePoolItems(inv, poolName, player);

    player.openInventory(inv);
    if (playSound) playSound(player, plugin.getConfigManager().getOpenSound());
  }

  private void fillBackground(Inventory inv, String poolName) {
    for (Map.Entry<Integer, GuiConfig.SlotConfig> entry : getGuiSlots(poolName).entrySet()) {
      int slot = entry.getKey();
      if (slot >= inv.getSize()) continue;
      GuiConfig.SlotConfig cfg = entry.getValue();
      if ("pool".equals(cfg.type())) continue;

      Material mat = MaterialHelper.getMaterial(cfg.material());
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        if (cfg.name() != null && !cfg.name().isBlank())
          meta.displayName(MM.deserialize(cfg.name()).decoration(TextDecoration.ITALIC, false));
        if (cfg.lore() != null && !cfg.lore().isEmpty())
          meta.lore(parseLore(cfg.lore()));
        for (ItemFlag flag : ItemFlag.values()) {
          try { meta.addItemFlags(flag); } catch (Exception ignored) {}
        }
        item.setItemMeta(meta);
      }
      inv.setItem(slot, item);
    }
  }

  private void placePoolItems(Inventory inv, String poolName, Player player) {
    List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);
    List<ItemStack>  cached     = cachedItemsMap.getOrDefault(poolName, Collections.emptyList());
    Map<Integer, StoreItem> slotMap = buildSlotToItemMap(activeItems, poolName, inv.getSize());

    for (Map.Entry<Integer, StoreItem> entry : slotMap.entrySet()) {
      int targetSlot = entry.getKey();
      StoreItem sItem = entry.getValue();
      int index = activeItems.indexOf(sItem);
      ItemStack item = getItemForPlayer(cached, index, sItem, poolName, player);
      if (item != null) inv.setItem(targetSlot, item.clone());
    }
  }

  private ItemStack getItemForPlayer(List<ItemStack> cached, int index,
                     StoreItem sItem, String poolName, Player player) {
    ItemStack base = (index >= 0 && index < cached.size()) ? cached.get(index) : null;
    if (base == null) return null;

    boolean isAdmin    = player.hasPermission("nyxonstore.admin");
    boolean hasVip     = player.hasPermission("nyxonstore.vip");
    boolean isRevealed = plugin.getDataManager().isItemRevealed(player.getUniqueId(), poolName, sItem.id());
    boolean isSoldOut  = sItem.maxPurchases() > 0
        && plugin.getDataManager().getPurchaseCount(poolName, sItem.id()) >= sItem.maxPurchases();

    if (!isAdmin) {
      if (!isRevealed && sItem.hidden()) return buildPlaceholderItem(plugin.getConfigManager().getHiddenItem());
      if (sItem.vip() && !hasVip)        return buildPlaceholderItem(plugin.getConfigManager().getVipItem());
    }
    if (isSoldOut) return buildPlaceholderItem(plugin.getConfigManager().getSoldOutItem());
    return base;
  }

  private Map<Integer, StoreItem> buildSlotToItemMap(List<StoreItem> activeItems,
                            String poolName, int invSize) {
    Map<Integer, StoreItem> map           = new LinkedHashMap<>();
    Set<Integer>            occupiedSlots = new HashSet<>();

    for (StoreItem sItem : activeItems) {
      if (sItem.slot() >= 0 && sItem.slot() < invSize) {
        map.put(sItem.slot(), sItem);
        occupiedSlots.add(sItem.slot());
      }
    }

    List<Integer> poolSlots = plugin.getItemPoolManager().getPoolSlots(poolName);
    int poolIdx = 0;
    for (StoreItem sItem : activeItems) {
      if (sItem.slot() != -1) continue;
      while (poolIdx < poolSlots.size() && occupiedSlots.contains(poolSlots.get(poolIdx))) poolIdx++;
      if (poolIdx >= poolSlots.size()) break;
      int targetSlot = poolSlots.get(poolIdx++);
      map.put(targetSlot, sItem);
      occupiedSlots.add(targetSlot);
    }
    return map;
  }

  public ItemStack buildConfigItem(String materialStr, String texture, String owner,
                   String name, List<String> lore) {
    Material mat  = MaterialHelper.getMaterial(materialStr);
    ItemStack item = new ItemStack(mat);
    if (texture != null && !texture.isBlank())
      HeadHelper.applyTexture(item, texture);
    else if (owner != null && !owner.isBlank())
      HeadHelper.applyOwner(item, owner);

    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      if (name != null && !name.isBlank())
        meta.displayName(MM.deserialize(name).decoration(TextDecoration.ITALIC, false));
      if (lore != null && !lore.isEmpty())
        meta.lore(parseLore(lore));
      item.setItemMeta(meta);
    }
    return item;
  }

  private ItemStack buildPlaceholderItem(PlaceholderItem pi) {
    return buildConfigItem(pi.material(), pi.texture(), pi.owner(), pi.name(), pi.lore());
  }

  private void applyStoreLore(ItemStack item, StoreItem sItem, String currency) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return;
    List<Component> lore = meta.hasLore() && meta.lore() != null
        ? new ArrayList<>(meta.lore()) : new ArrayList<>();
    String priceLine = plugin.getGuiConfig().getLorePrice()
        .replace("{price}", formatPrice(sItem.price()))
        .replace("{currency}", currency);
    lore.add(MM.deserialize(priceLine).decoration(TextDecoration.ITALIC, false));
    lore.add(Component.empty());
    lore.add(MM.deserialize(plugin.getGuiConfig().getLorePurchaseHint())
        .decoration(TextDecoration.ITALIC, false));
    meta.lore(lore);
    if (sItem.displayName() != null && !sItem.displayName().isBlank())
      meta.displayName(MM.deserialize(sItem.displayName()).decoration(TextDecoration.ITALIC, false));
    item.setItemMeta(meta);
  }

  private void openConfirmation(Player player, StoreItem sItem, String poolName) {
    ConfirmationConfig cc  = plugin.getConfirmationConfig();
    ConfirmationHolder holder = new ConfirmationHolder(sItem, poolName);
    Inventory inv = Bukkit.createInventory(holder, cc.getSize(),
        MM.deserialize(cc.getTitle()));

    ConfirmationConfig.SlotItem fi = cc.getFillerItem();
    ItemStack filler = buildConfigItem(fi.material(), fi.texture(), fi.owner(), fi.name(), fi.lore());
    for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler.clone());

    cc.getDecoSlots().forEach((slot, si) -> {
      if (slot >= 0 && slot < inv.getSize())
        inv.setItem(slot, buildConfigItem(si.material(), si.texture(), si.owner(), si.name(), si.lore()));
    });

    if (cc.getConfirmSlot() >= 0 && cc.getConfirmSlot() < inv.getSize()) {
      ConfirmationConfig.SlotItem ci = cc.getConfirmItem();
      inv.setItem(cc.getConfirmSlot(),
          buildConfigItem(ci.material(), ci.texture(), ci.owner(), ci.name(), ci.lore()));
    }

    if (cc.getCancelSlot() >= 0 && cc.getCancelSlot() < inv.getSize()) {
      ConfirmationConfig.SlotItem ca = cc.getCancelItem();
      inv.setItem(cc.getCancelSlot(),
          buildConfigItem(ca.material(), ca.texture(), ca.owner(), ca.name(), ca.lore()));
    }

    ItemStack info = getCachedItemForDisplay(poolName, sItem);
    if (info != null && cc.getInfoSlot() >= 0 && cc.getInfoSlot() < inv.getSize())
      inv.setItem(cc.getInfoSlot(), info);

    player.openInventory(inv);
  }

  private ItemStack getCachedItemForDisplay(String poolName, StoreItem sItem) {
    if (!cachedItemsMap.containsKey(poolName)) buildCache(poolName);
    List<StoreItem> active = plugin.getItemPoolManager().getActiveItems(poolName);
    List<ItemStack>  cached = cachedItemsMap.get(poolName);
    if (cached == null) return null;
    for (int i = 0; i < active.size() && i < cached.size(); i++) {
      if (active.get(i) != null && active.get(i).id().equals(sItem.id())) {
        ItemStack it = cached.get(i);
        return it != null ? it.clone() : null;
      }
    }
    return null;
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    Inventory top    = event.getView().getTopInventory();
    Inventory clicked = event.getClickedInventory();
    InventoryHolder holder = top.getHolder();

    if (holder instanceof ConfirmationHolder ch) {
      if (clicked != null && clicked.equals(top)) {
        event.setCancelled(true);
        handleConfirmationClick(event.getSlot(), player, ch);
      }
      return;
    }

    if (holder instanceof StoreHolder sh) {
      if (clicked != null && clicked.equals(top)) {
        event.setCancelled(true);
        handleStoreClick(event.getSlot(), player, sh);
      }
    }
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    InventoryHolder holder = event.getView().getTopInventory().getHolder();
    if (!(holder instanceof StoreHolder) && !(holder instanceof ConfirmationHolder)) return;
    Inventory top = event.getView().getTopInventory();
    for (int slot : event.getRawSlots()) {
      if (slot < top.getSize()) { event.setCancelled(true); return; }
    }
  }

  private void handleStoreClick(int slot, Player player, StoreHolder sh) {
    String poolName = sh.getPoolName();
    if (sh.getGeneration() != plugin.getItemPoolManager().getPoolGeneration(poolName)) {
      player.closeInventory();
      return;
    }

    List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);
    StoreItem sItem = buildSlotToItemMap(activeItems, poolName, getGuiSize(poolName)).get(slot);
    if (sItem == null) return;

    boolean isAdmin = player.hasPermission("nyxonstore.admin");
    boolean hasVip  = player.hasPermission("nyxonstore.vip");
    boolean isRevealed = plugin.getDataManager().isItemRevealed(player.getUniqueId(), poolName, sItem.id());

    if (!isAdmin) {
      if (!isRevealed && sItem.hidden()) {
        plugin.getDataManager().revealItem(player.getUniqueId(), poolName, sItem.id());
        playSound(player, plugin.getConfigManager().getRevealSound());
        buildCache(poolName);
        openSilent(player, poolName);
        return;
      }
      if (sItem.vip() && !hasVip) return;
    }

    if (sItem.maxPurchases() > 0
        && plugin.getDataManager().getPurchaseCount(poolName, sItem.id()) >= sItem.maxPurchases()) return;

    if (plugin.getConfirmationConfig().isEnabled()) {
      openConfirmation(player, sItem, poolName);
    } else {
      handlePurchase(player, sItem, poolName);
    }
  }

  private void handleConfirmationClick(int slot, Player player, ConfirmationHolder ch) {
    ConfirmationConfig cc = plugin.getConfirmationConfig();
    if (slot == cc.getConfirmSlot())
      handlePurchase(player, ch.getItem(), ch.getPoolName());
    else if (slot == cc.getCancelSlot())
      open(player, ch.getPoolName());
  }

  private void handlePurchase(Player player, StoreItem sItem, String poolName) {
    if (sItem.maxPurchases() > 0
        && plugin.getDataManager().getPurchaseCount(poolName, sItem.id()) >= sItem.maxPurchases()) {
      player.sendMessage(MM.deserialize(plugin.getConfigManager().getMessage("already-sold")));
      return;
    }

    boolean isCommandItem = "command".equalsIgnoreCase(sItem.type());

    if (!isCommandItem && player.getInventory().firstEmpty() == -1) {
      player.sendMessage(MM.deserialize(plugin.getConfigManager().getMessage("inventory-full")));
      return;
    }

    String currencyId = (sItem.currency() != null && !sItem.currency().isBlank())
        ? sItem.currency() : plugin.getConfigManager().getCurrency();
    double price      = sItem.price();
    double balance    = readBalance(player, currencyId);

    if (balance < 0) {
      plugin.getLogger().warning("[ModernStore/Purchase] Could not read balance for player '"
          + player.getName() + "', currency='" + currencyId
          + "'. PlaceholderAPI/CoinsEngine may be missing.");
      player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
          + "<red>Could not read your balance. Contact an admin.</red>"));
      return;
    }

    if (balance < price) {
      String msg = plugin.getConfigManager().getMessage("not-enough-coins")
          .replace("{currency}", currencyId)
          .replace("{price}", formatPrice(price));
      player.sendMessage(MM.deserialize(msg));
      return;
    }

    if (isCommandItem) {
      if (sItem.commands() == null || sItem.commands().isEmpty()) {
        player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
            + "<red>Command item has no commands configured. Contact an admin.</red>"));
        return;
      }
    } else {
      ItemStack item = resolveItemStack(sItem);
      if (item == null || item.getType() == Material.AIR) {
        plugin.getLogger().warning("[ModernStore/Purchase] Could not resolve item '" + sItem.id()
            + "' for player '" + player.getName() + "' (pool=" + poolName + ").");
        player.sendMessage(MM.deserialize(plugin.getConfigManager().getPrefix()
            + "<red>Could not give the item. Contact an admin.</red>"));
        return;
      }
    }

    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
        currencyId + " take " + player.getName() + " " + price);

    if (isCommandItem) {
      for (String rawCmd : sItem.commands()) {
        String cmd = rawCmd.replace("{player}", player.getName())
                   .replace("{player_name}", player.getName());
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (sItem.consoleCommands()) {
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else {
          Bukkit.dispatchCommand(player, cmd);
        }
      }
    } else {
      ItemStack item = resolveItemStack(sItem);
      player.getInventory().addItem(item);
    }
    plugin.getDataManager().incrementPurchaseCount(poolName, sItem.id());
    plugin.getDataManager().saveDataAsync();

    String itemLabel = resolveItemLabel(sItem);
    plugin.getLogManager().logPurchase(player, poolName, sItem.id(), itemLabel, price);

    String msg = plugin.getConfigManager().getMessage("purchased")
        .replace("{currency}", currencyId)
        .replace("{price}", formatPrice(price))
        .replace("{item}", itemLabel);
    player.sendMessage(MM.deserialize(msg));
    playSound(player, plugin.getConfigManager().getPurchaseSound());

    player.closeInventory();
    buildCache(poolName);
    refreshViewers(poolName);
  }

  private double readBalance(Player player, String currency) {
    if (!plugin.isPlaceholderApiEnabled()) return -1;
    try {
      String raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(
          player, "%coinsengine_balance_raw_" + currency + "%");
      if (raw.startsWith("%")) {
        raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(
            player, "%coinsengine_balance_" + currency + "%");
        if (raw.startsWith("%")) return -1;
      }
      return Double.parseDouble(raw.replaceAll("[^0-9.\\-]", ""));
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  public ItemStack resolveItemStack(StoreItem sItem) {
    return switch (sItem.type().toLowerCase()) {
      case "mmoitems"   -> fetchMMOItem(sItem.mmoItemType(), sItem.mmoItemId());
      case "itemsadder" -> fetchItemsAdderItem(sItem.itemsadderId());
      case "command"    -> {
        String mat = sItem.displayMaterial() != null && !sItem.displayMaterial().isBlank()
            ? sItem.displayMaterial() : "COMMAND_BLOCK";
        Material m = MaterialHelper.getMaterial(mat);
        if (m == Material.BARRIER && !mat.equalsIgnoreCase("barrier")) yield null;
        yield new ItemStack(m, sItem.amount() > 0 ? sItem.amount() : 1);
      }
      default -> {
        Material mat = MaterialHelper.getMaterial(sItem.material());
        if (mat == Material.BARRIER && !sItem.material().equalsIgnoreCase("barrier")) yield null;
        yield new ItemStack(mat, sItem.amount());
      }
    };
  }

  private ItemStack fetchMMOItem(String type, String id) {
    if (!plugin.isMmoItemsEnabled()) return null;
    try {
      net.Indyuce.mmoitems.MMOItems mmoItems = net.Indyuce.mmoitems.MMOItems.plugin;
      net.Indyuce.mmoitems.api.Type mmoType  = mmoItems.getTypes().get(type.toUpperCase());
      if (mmoType == null) return null;
      return mmoItems.getItem(mmoType, id.toUpperCase());
    } catch (Exception e) {
      plugin.getLogger().warning("[ModernStore/GUI] MMOItem '" + type + "/" + id
          + "' could not be fetched: " + e.getMessage());
      return null;
    }
  }

  private ItemStack fetchItemsAdderItem(String id) {
    if (!plugin.isItemsAdderEnabled()) return null;
    try {
      Object cs = Class.forName("dev.lone.itemsadder.api.CustomStack")
          .getMethod("getInstance", String.class).invoke(null, id);
      if (cs == null) return null;
      return (ItemStack) cs.getClass().getMethod("getItemStack").invoke(cs);
    } catch (Exception e) {
      plugin.getLogger().warning("[ModernStore/GUI] ItemsAdder item '" + id
          + "' could not be fetched: " + e.getMessage());
      return null;
    }
  }

  private String resolveItemLabel(StoreItem sItem) {
    return switch (sItem.type().toLowerCase()) {
      case "mmoitems"   -> sItem.mmoItemId();
      case "itemsadder" -> sItem.itemsadderId();
      case "command"    -> "[Command] " + sItem.id();
      default           -> sItem.material();
    };
  }

  public void refreshViewers(String poolName) {
    for (Player p : Bukkit.getOnlinePlayers()) {
      Inventory top = p.getOpenInventory().getTopInventory();
      InventoryHolder h = top != null ? top.getHolder() : null;
      if (h instanceof StoreHolder sh && sh.getPoolName().equalsIgnoreCase(poolName)) {
        openSilent(p, poolName);
      } else if (h instanceof ConfirmationHolder ch && ch.getPoolName().equalsIgnoreCase(poolName)) {
        openSilent(p, poolName);
      }
    }
  }

  public void closeAll() {
    for (Player p : Bukkit.getOnlinePlayers()) {
      Inventory top = p.getOpenInventory().getTopInventory();
      if (top == null) continue;
      InventoryHolder h = top.getHolder();
      if (h instanceof StoreHolder || h instanceof ConfirmationHolder) p.closeInventory();
    }
  }

  private int getGuiSize(String poolName) {
    PoolConfig.CustomGuiConfig c = plugin.getPoolConfig().getCustomGui(poolName);
    return c != null ? c.getSize() : plugin.getGuiConfig().getSize();
  }

  private Component getGuiTitle(String poolName) {
    PoolConfig.CustomGuiConfig c = plugin.getPoolConfig().getCustomGui(poolName);
    return MM.deserialize(c != null ? c.getTitle() : plugin.getGuiConfig().getTitle());
  }

  private Map<Integer, GuiConfig.SlotConfig> getGuiSlots(String poolName) {
    PoolConfig.CustomGuiConfig c = plugin.getPoolConfig().getCustomGui(poolName);
    return c != null ? c.getSlots() : plugin.getGuiConfig().getSlots();
  }

  private List<Component> parseLore(List<String> lines) {
    List<Component> lore = new ArrayList<>(lines.size());
    for (String line : lines) lore.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
    return lore;
  }

  private String formatPrice(double price) {
    BigDecimal bd = BigDecimal.valueOf(price)
        .setScale(10, RoundingMode.HALF_UP)
        .stripTrailingZeros();
    if (bd.scale() < 0) bd = bd.setScale(0);
    return bd.toPlainString();
  }

  private void playSound(Player player, SoundConfig sc) {
    if (sc == null) return;
    try {
      Sound sound = Sound.valueOf(sc.sound());
      player.playSound(player.getLocation(), sound, sc.volume(), sc.pitch());
    } catch (IllegalArgumentException e) {
      plugin.getLogger().warning("[ModernStore/GUI] Invalid sound '" + sc.sound() + "' in config.");
    }
  }
}
