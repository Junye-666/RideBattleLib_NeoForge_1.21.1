package com.jpigeon.ridebattlelib.core.system.belt;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.TriggerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class BeltHandler {
    //右键变身逻辑
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER) return;

        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) return;

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        boolean inserted = false;
        for (ResourceLocation slotId : config.getSlotDefinitions().keySet()) {
            if (BeltSystem.INSTANCE.insertItem(player, slotId, heldItem.copy())) {
                heldItem.shrink(1);
                inserted = true;
                break; // 插入成功后立即终止循环
            }
            RideBattleLib.LOGGER.debug("存入物品到槽位: {} (必要: {})", slotId, config.getRequiredSlots().contains(slotId));
        }

        if (inserted) {
            BeltSystem.INSTANCE.syncBeltData(player);
            event.setCanceled(true);

            // 仅在TriggerType为AUTO时自动触发
            if (config.getTriggerType() == TriggerType.AUTO && BeltSystem.INSTANCE.validateItems(player, config.getRiderId())) {
                HenshinSystem.INSTANCE.henshin(player, config.getRiderId());
            }
        }
    }
}
