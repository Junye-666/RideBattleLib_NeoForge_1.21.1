package com.jpigeon.ridebattlelib.core.system.belt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Consumer;

/**
 * @param allowedItems     允许插入的物品
 * @param onInsertCallback 插入时的回调
 * @param allowReplace     允许被新物品自动替换
 * @param isAuxSlot       是否是辅助槽
 */
public record SlotDefinition(List<Item> allowedItems, Consumer<Player> onInsertCallback, boolean allowReplace, boolean isAuxSlot, boolean isRequired) {}