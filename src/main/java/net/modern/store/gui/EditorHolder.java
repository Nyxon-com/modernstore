//made with ❤ by nyxon
package net.modern.store.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class EditorHolder implements InventoryHolder {

    private final PoolEditorGUI.MenuState state;
    private final String poolName;
    private final String itemId;
    private final String pendingTextInput;
    private final String tempText;

    public EditorHolder(PoolEditorGUI.MenuState state,
                        String poolName,
                        String itemId,
                        String pendingTextInput,
                        String tempText) {
        this.state            = state;
        this.poolName         = poolName;
        this.itemId           = itemId;
        this.pendingTextInput = pendingTextInput;
        this.tempText         = tempText;
    }

    public PoolEditorGUI.MenuState getState()            { return state;            }
    public String                  getPoolName()         { return poolName;         }
    public String                  getItemId()           { return itemId;           }
    public String                  getPendingTextInput() { return pendingTextInput; }
    public String                  getTempText()         { return tempText;         }

    @Override
    public Inventory getInventory() { return null; }
}
