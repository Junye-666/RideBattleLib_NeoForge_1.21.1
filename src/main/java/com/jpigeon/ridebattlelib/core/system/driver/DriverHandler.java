package com.jpigeon.ridebattlelib.core.system.driver;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.SyncManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.DriverActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID, value = Dist.DEDICATED_SERVER)
public class DriverHandler {
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getSide() != LogicalSide.SERVER) return;

        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        if (heldItem.isEmpty()) return;

        // 使用修改后的查找方法，兼容变身状态
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        boolean isTransformed = HenshinSystem.INSTANCE.isTransformed(player);

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("右键插入物品 - 玩家: {}, 变身状态: {}, 骑士: {}",
                    player.getName().getString(), isTransformed, config.getRiderId());
        }

        boolean inserted = false;
        // 先尝试主驱动器槽位
        for (ResourceLocation slotId : config.getSlotDefinitions().keySet()) {
            if (DriverSystem.INSTANCE.insertItem(player, slotId, heldItem.copy())) {
                heldItem.shrink(1);
                inserted = true;
                break;
            }
        }

        // 再尝试辅助驱动器槽位
        if (!inserted && config.hasAuxDriverEquipped(player)) {
            for (ResourceLocation slotId : config.getAuxSlotDefinitions().keySet()) {
                if (DriverSystem.INSTANCE.insertItem(player, slotId, heldItem.copy())) {
                    heldItem.shrink(1);
                    inserted = true;
                    break;
                }
            }
        }

        if (inserted) {
            if (player instanceof ServerPlayer serverPlayer) {
                SyncManager.INSTANCE.syncDriverData(serverPlayer);
            }
            FormConfig formConfig = config.getActiveFormConfig(player);
            if (formConfig != null && Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("形态触发类型: {}", formConfig.getTriggerType());
            }
            // 添加 null 检查
            if (formConfig != null && formConfig.getTriggerType() == TriggerType.AUTO) {
                if (Config.DEBUG_MODE.get()){
                    RideBattleLib.LOGGER.debug("自动触发 - 玩家状态: 变身={}, 驱动器={}", HenshinSystem.INSTANCE.isTransformed(player), config.getRiderId());
                }
                PacketHandler.sendToServer(new DriverActionPacket(player.getUUID()));
            }

            event.setCanceled(true);
        }
    }
}