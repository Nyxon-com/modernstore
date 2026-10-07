//made with ❤ by nyxon
package net.nyxon.store.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class StoreHolder implements InventoryHolder {

    private final String poolName;
    private final long   generation;

    public StoreHolder(String poolName, long generation) {
        this.poolName   = poolName;
        this.generation = generation;
    }

    public String getPoolName()   { return poolName;   }
    public long   getGeneration() { return generation; }

    @Override
    public Inventory getInventory() { return null; }
}
