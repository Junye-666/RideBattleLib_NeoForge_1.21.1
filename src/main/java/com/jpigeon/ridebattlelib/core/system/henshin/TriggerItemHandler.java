package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
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

        if (player.level().isClientSide()) return;

        if (config != null && heldItem.is(config.getTriggerItem())) {
            // 取消事件传播，避免物品被消耗
            event.setCanceled(true);

            // 仅在客户端触发一次
            if (player.level().isClientSide()) return;

            // 触发变身逻辑
            if (BeltSystem.INSTANCE.validateItems(player, config.getRiderId())) {
                HenshinSystem.INSTANCE.henshin(player, config.getRiderId());
                RideBattleLib.LOGGER.debug("通过TriggerItem触发变身: {}", config.getRiderId());
            }

            // 强制恢复物品数量（防止NBT修改）
            if (!player.isCreative()) {
                heldItem.setCount(heldItem.getCount() + 1);
            }
        }
    }
}
