package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SwitchFormPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SyncHenshinStatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;

public class DriverActionManager {
    public static final DriverActionManager INSTANCE = new DriverActionManager();

    public void prepareHenshin(Player player, ResourceLocation formId) {
        RideBattleLib.LOGGER.debug("玩家 {} 进入变身缓冲阶段", player.getName());
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        // 设置变身状态
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setHenshinState(HenshinState.TRANSFORMING);
        data.setPendingFormId(formId);

        RideBattleLib.LOGGER.info("设置待处理形态: player={}, form={}",
                player.getName().getString(), formId);

        // 同步状态
        if (player.level().isClientSide) {
            // 客户端发送同步请求
            PacketHandler.sendToServer(new SyncHenshinStatePacket(
                    HenshinState.TRANSFORMING,
                    formId
            ));
        } else if (player instanceof ServerPlayer serverPlayer) {
            // 服务端直接同步
            HenshinSystem.syncHenshinState(serverPlayer);
        }

        HenshinEvent.Pre preHenshin = new HenshinEvent.Pre(player, config.getRiderId(), formId, LogicalSide.SERVER);
        NeoForge.EVENT_BUS.post(preHenshin);
        if (preHenshin.isCanceled()) {
            RideBattleLib.LOGGER.info("取消变身");
            cancelHenshin(player);
        }
    }

    public void proceedHenshin(Player player, RiderConfig config) {
        RideBattleLib.LOGGER.debug("使玩家 {} 继续变身 {}", player.getName(), config.getRiderId());
        if (HenshinSystem.INSTANCE.isTransformed(player)) return;
        PacketHandler.sendToServer(new HenshinPacket(config.getRiderId()));
        RideBattleLib.LOGGER.info("发送变身包: {}", config.getRiderId());
    }

    public void proceedFormSwitch(Player player, ResourceLocation newFormId) {
        RideBattleLib.LOGGER.debug("玩家 {} 进入形态切换阶段", player.getName());
        RideBattleLib.LOGGER.info("发送形态切换包: {}", newFormId);
        PacketHandler.sendToServer(new SwitchFormPacket(newFormId));
    }

    public void completeTransformation(Player player) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        ResourceLocation formId = data.getPendingFormId();

        if (formId == null) {
            RideBattleLib.LOGGER.error("尝试完成变身但未设置目标形态");
            return;
        }

        RideBattleLib.LOGGER.info("完成变身序列: player={}, form={}",
                player.getName().getString(), formId);

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
            HenshinSystem.syncTransformedState(serverPlayer);
        }
    }

    public void cancelHenshin(Player player) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        if (data.getHenshinState() == HenshinState.TRANSFORMING) {
            data.setHenshinState(HenshinState.IDLE);
            data.setPendingFormId(null);

            if (player instanceof ServerPlayer serverPlayer) {
                HenshinSystem.syncHenshinState(serverPlayer);
                HenshinSystem.syncTransformedState(serverPlayer);
            }
        }
    }
}
