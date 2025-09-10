package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SwitchFormPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;

public class DriverActionManager {
    public static final DriverActionManager INSTANCE = new DriverActionManager();

    public void prepareHenshin(Player player, ResourceLocation formId) {
        RideBattleLib.LOGGER.debug("玩家 {} 进入变身缓冲阶段", player.getName());
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        RideBattleLib.LOGGER.info("设置待处理形态: player={}, form={}", player.getName().getString(), formId);

        HenshinEvent.Pre preHenshin = new HenshinEvent.Pre(player, config.getRiderId(), formId);
        NeoForge.EVENT_BUS.post(preHenshin);
        if (preHenshin.isCanceled()) {
            RideBattleLib.LOGGER.info("取消变身");
            cancelHenshin(player);
        }
    }

    public void proceedHenshin(Player player, RiderConfig config) {
        RideBattleLib.LOGGER.debug("使玩家 {} 继续变身 {}", player.getName(), config.getRiderId());
        if (HenshinSystem.INSTANCE.isTransformed(player)) return;
        PacketHandler.sendToServer(new HenshinPacket(player.getUUID(), config.getRiderId()));
        RideBattleLib.LOGGER.info("发送变身包: {}", config.getRiderId());
    }

    public void proceedFormSwitch(Player player, ResourceLocation newFormId) {
        RideBattleLib.LOGGER.debug("玩家 {} 进入形态切换阶段", player.getName());
        RideBattleLib.LOGGER.info("发送形态切换包: {}", newFormId);
        PacketHandler.sendToServer(new SwitchFormPacket(player.getUUID(), newFormId));
    }

    public void completeTransformation(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        ResourceLocation formId = data.getPendingFormId();
        RideBattleLib.LOGGER.debug("当前状态: {}, 形态ID: {}", data.getHenshinState(), formId);
        if (formId == null) {
            RideBattleLib.LOGGER.error("尝试完成变身但未设置目标形态");
            return;
        }

        RideBattleLib.LOGGER.info("完成变身序列: player={}, form={}", player.getName().getString(), formId);

        if (!HenshinSystem.INSTANCE.isTransformed(player)) {
            proceedHenshin(player, Objects.requireNonNull(RiderConfig.findActiveDriverConfig(player)));
        } else {
            proceedFormSwitch(player, formId);
        }

        // 重置状态
        data.setHenshinState(HenshinState.TRANSFORMED);
        data.setPendingFormId(null);

        // 同步状态
        if (player instanceof ServerPlayer serverPlayer) {
            HenshinSystem.syncHenshinState(serverPlayer);
        }
    }

    public void cancelHenshin(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        if (data.getHenshinState() == HenshinState.TRANSFORMING) {
            data.setHenshinState(HenshinState.IDLE);
            data.setPendingFormId(null);

            if (player instanceof ServerPlayer serverPlayer) {
                HenshinSystem.syncHenshinState(serverPlayer);
            }
        }
    }
}
