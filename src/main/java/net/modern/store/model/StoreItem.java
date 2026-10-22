//made with love by nyxon

package net.modern.store.model;

import java.util.List;

public record StoreItem(
    String id,
    String type,
    String mmoItemType,
    String mmoItemId,
    String material,
    String itemsadderId,
    int amount,
    double price,
    int weight,
    boolean vip,
    boolean hidden,
    int maxPurchases,
    int slot,
    String displayName,
    String currency,
    String displayMaterial,
    List<String> commands,
    boolean consoleCommands
) {}
