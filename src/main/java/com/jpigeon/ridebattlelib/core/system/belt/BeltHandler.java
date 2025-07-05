package com.jpigeon.ridebattlelib.core.system.belt;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.trigger.TriggerType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;

@EventBusSubscriber(modid = RideBattleLib.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.DEDICATED_SERVER)
public class BeltHandler {
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER) return;

        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) return;

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        boolean inserted = false;
        // 先尝试主驱动器槽位
        for (ResourceLocation slotId : config.getSlotDefinitions().keySet()) {
            if (BeltSystem.INSTANCE.insertItem(player, slotId, heldItem.copy())) {
                heldItem.shrink(1);
                inserted = true;
                break;
            }
        }

        // 再尝试辅助驱动器槽位
        if (!inserted && config.hasAuxDriverEquipped(player)) {
            for (ResourceLocation slotId : config.getAuxSlotDefinitions().keySet()) {
                if (BeltSystem.INSTANCE.insertItem(player, slotId, heldItem.copy())) {
                    heldItem.shrink(1);
                    inserted = true;
                    break;
                }
            }
        }

        if (inserted) {
            BeltSystem.INSTANCE.syncBeltData(player);
            event.setCanceled(true);

            if (config.getTriggerType() == TriggerType.AUTO) {
                if (HenshinSystem.INSTANCE.isTransformed(player)) {
                    Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);
                    ResourceLocation newFormId = config.matchForm(player, currentBelt);
                    HenshinSystem.INSTANCE.switchForm(player, newFormId);
                }
            }
        }
    }
}
