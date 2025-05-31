package com.jpigeon.ridebattlelib.core.system.belt;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
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
        for (ResourceLocation slotId : config.getSlotDefinitions().keySet()) {
            if (BeltSystem.INSTANCE.insertItem(player, slotId, heldItem.copy())) {
                heldItem.shrink(1);
                inserted = true;

                // 新增：在变身状态下插入物品时自动切换形态
                if (HenshinSystem.INSTANCE.isTransformed(player) && config.getTriggerType() == TriggerType.AUTO) {
                    Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
                    ResourceLocation newFormId = config.matchForm(beltItems);
                }

                break; // 插入成功后立即终止循环
            }
            RideBattleLib.LOGGER.debug("存入物品到槽位: {} (必要: {})", slotId, config.getRequiredSlots().contains(slotId));
        }

        if (inserted) {
            BeltSystem.INSTANCE.syncBeltData(player);
            event.setCanceled(true);

            if (config.getTriggerType() == TriggerType.AUTO) {
                if (HenshinSystem.INSTANCE.isTransformed(player)) {
                    // 获取当前腰带状态
                    Map<ResourceLocation, ItemStack> currentBelt = BeltSystem.INSTANCE.getBeltItems(player);

                    // 尝试匹配新形态
                    ResourceLocation newFormId = config.matchForm(currentBelt);

                    // 处理可能为空的情况
                    HenshinSystem.INSTANCE.switchForm(player, newFormId);
                }
            }
        }
    }

    public static void handleAutoFormSwitch(Player player, ResourceLocation newFormId) {
        HenshinSystem.INSTANCE.switchForm(player, newFormId);
    }

    public static void onAutoItemExtracted(Player player, ResourceLocation slotId) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config != null && config.getTriggerType() == TriggerType.AUTO &&
                HenshinSystem.INSTANCE.isTransformed(player)) {

            Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
            ResourceLocation newFormId = config.matchForm(beltItems);

            // 处理可能为空的情况
            HenshinSystem.INSTANCE.switchForm(player, newFormId);
        }
    }
}