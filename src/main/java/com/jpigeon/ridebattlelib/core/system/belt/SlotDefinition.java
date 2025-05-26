package com.jpigeon.ridebattlelib.core.system.belt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Consumer;

public class SlotDefinition {
    private final List<Item> allowedItems; // 允许插入的物品
    private final Consumer<Player> onInsertCallback; // 插入时的回调

    public SlotDefinition(List<Item> allowedItems, Consumer<Player> onInsertCallback) {
        this.allowedItems = allowedItems;
        this.onInsertCallback = onInsertCallback;
    }

    public List<Item> getAllowedItems() {
        return allowedItems;
    }

    public Consumer<Player> getOnInsertCallback() {
        return onInsertCallback;
    }
}