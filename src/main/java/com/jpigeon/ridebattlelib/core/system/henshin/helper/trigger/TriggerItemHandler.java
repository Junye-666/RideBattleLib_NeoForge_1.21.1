package com.jpigeon.ridebattlelib.core.system.henshin.helper.trigger;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;

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

            // 触发变身逻辑
            if (config.getTriggerType() == TriggerType.ITEM) {
                // 触发变身逻辑
                if (BeltSystem.INSTANCE.validateItems(player, config.getRiderId())
                        && (!config.hasAuxDriverEquipped(player) || config.getAuxSlotDefinitions().isEmpty())) {
                    if (HenshinSystem.INSTANCE.isTransformed(player)) {
                        // 已变身状态下使用触发物品：切换形态
                        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
                        ResourceLocation newFormId = config.matchForm(player, beltItems);
                        handleItemFormSwitch(player, newFormId);
                    } else {
                        // 未变身：正常触发变身
                        HenshinSystem.INSTANCE.henshin(player, config.getRiderId());
                    }
                }
            }

            // 强制恢复物品数量（防止NBT修改）
            if (!player.isCreative()) {
                heldItem.setCount(heldItem.getCount() + 1);
            }
        }
    }

    public static void handleItemFormSwitch(Player player, ResourceLocation newFormId) {
        RideBattleLib.LOGGER.info("处理ITEM类型形态切换: {}", newFormId);
        HenshinHelper.INSTANCE.performFormSwitch(player, newFormId);
    }
}
