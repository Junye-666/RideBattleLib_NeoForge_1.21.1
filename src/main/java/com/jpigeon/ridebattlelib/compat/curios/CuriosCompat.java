package com.jpigeon.ridebattlelib.compat.curios;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public class CuriosCompat {
    private static boolean curiosLoaded = false;

    static {
        try {
            Class.forName("top.theillusivec4.curios.api.CuriosApi");
            curiosLoaded = true;
            RideBattleLib.LOGGER.info("Curios API detected, enabling compatibility");
        } catch (ClassNotFoundException e) {
            curiosLoaded = false;
            RideBattleLib.LOGGER.debug("Curios API not found, using vanilla equipment only");
        }
    }

    /**
     * 检查玩家在指定Curios槽位中是否装备了特定物品
     * @param player 玩家
     * @param item 要检查的物品
     * @param curiosSlotId Curios槽位ID
     * @return 如果槽位中存在该物品则返回true
     */
    public static boolean hasItemInCuriosSlot(Player player, Item item, String curiosSlotId) {
        if (!curiosLoaded || item == null || curiosSlotId == null) {
            if (Config.DEBUG_MODE.get() && curiosSlotId != null) {
                RideBattleLib.LOGGER.debug("Curios检测条件不满足: loaded={}, item={}, slot={}",
                        curiosLoaded, item, curiosSlotId);
            }
            return false;
        }

        try {
            return CuriosApi.getCuriosInventory(player)
                    .flatMap(inv -> inv.getStacksHandler(curiosSlotId))
                    .map(handler -> {
                        boolean found = false;
                        for (int i = 0; i < handler.getSlots(); i++) {
                            ItemStack stack = handler.getStacks().getStackInSlot(i);
                            if (stack.is(item)) {
                                if (Config.DEBUG_MODE.get()) {
                                    RideBattleLib.LOGGER.debug("在Curios槽位 {} 中找到物品: {}",
                                            curiosSlotId, BuiltInRegistries.ITEM.getKey(item));
                                }
                                found = true;
                                break;
                            }
                        }
                        return found;
                    })
                    .orElse(false);
        } catch (Exception e) {
            RideBattleLib.LOGGER.error("Curios槽位检查出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从Curios槽位获取物品
     * @param player 玩家
     * @param curiosSlotId Curios槽位ID
     * @return 槽位中的物品堆栈，如果为空则返回ItemStack.EMPTY
     */
    public static ItemStack getItemFromCuriosSlot(Player player, String curiosSlotId) {
        if (!curiosLoaded || curiosSlotId == null) return ItemStack.EMPTY;

        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.getStacksHandler(curiosSlotId))
                .map(handler -> {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        ItemStack stack = handler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) {
                            return stack;
                        }
                    }
                    return ItemStack.EMPTY;
                })
                .orElse(ItemStack.EMPTY);
    }

    public static boolean isCuriosSlotAvailable(Player player, String curiosSlotId) {
        if (!curiosLoaded || curiosSlotId == null) return false;

        return CuriosApi.getCuriosInventory(player)
                .flatMap(inv -> inv.getStacksHandler(curiosSlotId))
                .isPresent();
    }

    public static boolean isCuriosLoaded() {
        return curiosLoaded;
    }
}