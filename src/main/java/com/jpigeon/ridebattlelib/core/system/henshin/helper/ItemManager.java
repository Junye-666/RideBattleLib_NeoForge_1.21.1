package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.core.system.event.ItemGrantEvent;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

public class ItemManager {
    public static final ItemManager INSTANCE = new ItemManager();

    // 给予形态物品
    public void grantFormItems(Player player, ResourceLocation formId) {
        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig == null) {
            formConfig = DynamicFormConfig.getDynamicForm(formId);
        }
        if (formConfig != null) {
            for (ItemStack stack : formConfig.getGrantedItems()) {
                ItemGrantEvent.Pre preGrant = new ItemGrantEvent.Pre(player, stack, formConfig);
                NeoForge.EVENT_BUS.post(preGrant);
                if (preGrant.isCanceled()) return;

                if (!player.addItem(preGrant.getStack().copy())) {
                    player.drop(preGrant.getStack().copy(), false);

                    ItemGrantEvent.Post postGrant = new ItemGrantEvent.Post(player, preGrant.getStack().copy(), formConfig);
                    NeoForge.EVENT_BUS.post(postGrant);
                }
            }
        }
    }

    // 移除给予的物品
    public void removeGrantedItems(Player player, ResourceLocation formId) {
        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig == null) {
            formConfig = DynamicFormConfig.getDynamicForm(formId);
        }
        if (formConfig != null) {
            for (ItemStack grantedItem : formConfig.getGrantedItems()) {
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
