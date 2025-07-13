package com.jpigeon.ridebattlelib.core.system.henshin.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class TriggerItemHandler {
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) {
            return;
        }
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        ResourceLocation formId = data.getPendingFormId();
        FormConfig formConfig = RiderRegistry.getForm(formId);

        if (player.level().isClientSide()) return;
        if (heldItem.is(config.getTriggerItem())) {
            // 取消事件传播，避免物品被消耗
            event.setCanceled(true);

            // 触发变身逻辑
            if (formConfig != null && formConfig.getTriggerType() == TriggerType.ITEM) {
                RideBattleLib.LOGGER.debug("检测到ITEM驱动方式");
                // 触发驱动器
                RideBattleLib.LOGGER.info("物品触发 - 玩家状态: 变身={}, 驱动器={}", HenshinSystem.INSTANCE.isTransformed(player), config.getRiderId());
                HenshinSystem.INSTANCE.driverAction(player);
            }

            // 强制恢复物品数量（防止NBT修改）
            if (!player.isCreative()) {
                heldItem.setCount(heldItem.getCount() + 1);
            }
        }
    }
}
