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


    public void grantFormItems(Player player, ResourceLocation formId) {
        FormConfig formConfig = getFormConfig(player, formId);
        if (formConfig != null) {
            grantFormItemsInternal(player, formConfig);
        }
    }

    public void removeGrantedItems(Player player, ResourceLocation formId) {
        FormConfig formConfig = getFormConfig(player, formId);
        if (formConfig != null) {
            removeGrantedItemsInternal(player, formConfig);
        }
    }


    // 给予形态物品
    public void grantFormItemsInternal(Player player, FormConfig form) {
        if (form != null) {
            for (ItemStack stack : form.getGrantedItems()) {
                ItemGrantEvent.Pre preGrant = new ItemGrantEvent.Pre(player, stack, form);
                NeoForge.EVENT_BUS.post(preGrant);
                if (preGrant.isCanceled()) return;

                if (!player.addItem(preGrant.getStack().copy())) {
                    player.drop(preGrant.getStack().copy(), false);

                    ItemGrantEvent.Post postGrant = new ItemGrantEvent.Post(player, preGrant.getStack().copy(), form);
                    NeoForge.EVENT_BUS.post(postGrant);
                }
            }
        }
    }

    // 移除给予的物品
    public void removeGrantedItemsInternal(Player player, FormConfig form) {
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

    private FormConfig getFormConfig(Player player, ResourceLocation formId) {
        // 优先从玩家当前骑士获取
        FormConfig form = RiderRegistry.getForm(player, formId);

        // 如果没找到，尝试动态形态
        if (form == null) {
            form = DynamicFormConfig.getDynamicForm(formId);
        }

        return form;
    }
}
