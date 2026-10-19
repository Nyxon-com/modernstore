//made with ❤ by nyxon
package net.modern.store.gui;

import net.modern.store.model.StoreItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ConfirmationHolder implements InventoryHolder {

    private final StoreItem item;
    private final String    poolName;

    public ConfirmationHolder(StoreItem item, String poolName) {
        this.item     = item;
        this.poolName = poolName;
    }

    public StoreItem getItem()     { return item;     }
    public String    getPoolName() { return poolName; }

    @Override
    public Inventory getInventory() { return null; }
}
