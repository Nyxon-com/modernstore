//made with ❤ by nyxon
package net.nyxon.store.placeholder;

import net.nyxon.store.NyxonStore;
import net.nyxon.store.model.StoreItem;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NyxonStoreExpansion extends PlaceholderExpansion {

    private final NyxonStore plugin;

    public NyxonStoreExpansion(NyxonStore plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "nyxonstore"; }
    @Override public @NotNull String getAuthor()     { return "nyxon"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("default_pool"))
            return plugin.getConfigManager().getDefaultPool();

        String[] parts = params.split("_");
        if (parts.length < 2) return null;

        String poolName = parts[0];
        String action   = parts[1];

        if (action.equalsIgnoreCase("rotation") && parts.length >= 3
                && parts[2].equalsIgnoreCase("time")) {
            return handleRotationTime(poolName, parts);
        }

        if (action.equalsIgnoreCase("item") && parts.length >= 5) {
            return handleItemPlaceholder(poolName, parts);
        }

        return null;
    }

    private String handleRotationTime(String poolName, String[] parts) {
        long remaining = plugin.getDataManager().getNextRotationTime(poolName)
                - System.currentTimeMillis();
        if (remaining <= 0) return "0";

        long totalSeconds = remaining / 1000;
        long seconds      = totalSeconds % 60;
        long totalMinutes = totalSeconds / 60;
        long minutes      = totalMinutes % 60;
        long hours        = (totalMinutes / 60) % 24;
        long days         = totalMinutes / 60 / 24;

        if (parts.length >= 4 && parts[3].equalsIgnoreCase("seconds"))
            return String.valueOf(totalSeconds);

        if (parts.length >= 4 && parts[3].equalsIgnoreCase("formatted")) {
            if (days > 0)   return String.format("%dd %dh %dm", days, hours, minutes);
            if (hours > 0)  return String.format("%dh %dm %ds", hours, minutes, seconds);
            return String.format("%dm %ds", minutes, seconds);
        }

        return String.format("%dd %dh %dm", days, hours, minutes);
    }

    private String handleItemPlaceholder(String poolName, String[] parts) {
        int index;
        try { index = Integer.parseInt(parts[2]); }
        catch (NumberFormatException e) { return ""; }

        List<StoreItem> activeItems = plugin.getItemPoolManager().getActiveItems(poolName);
        if (index < 0 || index >= activeItems.size()) return "";

        StoreItem item     = activeItems.get(index);
        String    property = parts[3].toLowerCase();

        return switch (property) {
            case "name"      -> resolveLabel(item);
            case "price"     -> String.valueOf(item.price());
            case "type"      -> item.type();
            case "purchases" -> String.valueOf(
                    plugin.getDataManager().getPurchaseCount(poolName, item.id()));
            case "max"       -> String.valueOf(item.maxPurchases());
            case "soldout"   -> {
                boolean sold = item.maxPurchases() > 0
                        && plugin.getDataManager().getPurchaseCount(poolName, item.id())
                        >= item.maxPurchases();
                yield sold ? "yes" : "no";
            }
            default -> "";
        };
    }

    private String resolveLabel(StoreItem item) {
        return switch (item.type().toLowerCase()) {
            case "mmoitems"   -> item.mmoItemId()    != null ? item.mmoItemId()    : "";
            case "itemsadder" -> item.itemsadderId() != null ? item.itemsadderId() : "";
            default           -> item.material()     != null ? item.material()     : "";
        };
    }
}
