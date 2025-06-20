package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ItemManager {
    public static final ItemManager INSTANCE = new ItemManager();

    // 给予形态物品
    public void grantFormItems(Player player, ResourceLocation formId) {
        FormConfig form = RiderRegistry.getForm(formId);
        if (form != null) {
            for (ItemStack stack : form.getGrantedItems()) {
                if (!player.addItem(stack.copy())) {
                    player.drop(stack.copy(), false);
                }
            }
        }
    }

    // 移除给予的物品
    public void removeGrantedItems(Player player, ResourceLocation formId) {
        FormConfig form = RiderRegistry.getForm(formId);
        if (form != null) {
            for (ItemStack grantedItem : form.getGrantedItems()) {
                int countToRemove = grantedItem.getCount();

                // 只移除玩家背包中的物品
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (ItemStack.isSameItem(stack, grantedItem)) {
                        int removeAmount = Math.min(countToRemove, stack.getCount());
                        stack.shrink(removeAmount);
                        countToRemove -= removeAmount;

                        if (countToRemove <= 0) break;
                    }
                }
            }
        }
    }
}
