package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.AnimationEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SwitchFormPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Map;
import java.util.Objects;

public class DriverActionManager {
    public static final DriverActionManager INSTANCE = new DriverActionManager();

    public void prepareHenshin(Player player, ResourceLocation formId) {
        RideBattleLib.LOGGER.debug("玩家 {} 进入变身缓冲阶段", player.getName());
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        HenshinEvent.Pre preHenshin = new HenshinEvent.Pre(player, config.getRiderId(), formId);
        NeoForge.EVENT_BUS.post(preHenshin);

        if (preHenshin.isCanceled()) {
            RideBattleLib.LOGGER.info("取消变身");
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

    public void handleDriverAction(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
        ResourceLocation formId = config.matchForm(beltItems);
        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig == null) return;

        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);

        if (data.getHenshinState() == HenshinState.IDLE) {
            // 开始变身序列
            startTransformationSequence(player, config, formId);
        } else if (data.getHenshinState() == HenshinState.TRANSFORMED) {
            // 开始形态切换序列
            startFormSwitchSequence(player, config, formId);
        }
    }

    private void startTransformationSequence(Player player, RiderConfig config, ResourceLocation formId) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setHenshinState(HenshinState.TRANSFORMING);
        data.setPendingFormId(formId);

        // 触发动画事件
        NeoForge.EVENT_BUS.post(new AnimationEvent(
                player,
                config.getRiderId(),
                formId,
                AnimationPhase.INIT
        ));

        // 同步状态
        if (player instanceof ServerPlayer serverPlayer) {
            HenshinSystem.syncHenshinState(serverPlayer);
        }
    }

    private void startFormSwitchSequence(Player player, RiderConfig config, ResourceLocation newFormId) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setHenshinState(HenshinState.TRANSFORMING);
        data.setPendingFormId(newFormId);

        // 触发动画事件
        NeoForge.EVENT_BUS.post(new AnimationEvent(
                player,
                config.getRiderId(),
                newFormId,
                AnimationPhase.CONTINUE
        ));

        // 同步状态
        if (player instanceof ServerPlayer serverPlayer) {
            HenshinSystem.syncHenshinState(serverPlayer);
        }
    }

    public void completeTransformation(Player player) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        ResourceLocation pendingFormId = data.getPendingFormId();

        if (pendingFormId == null) {
            RideBattleLib.LOGGER.error("尝试完成变身但未设置目标形态");
            return;
        }

        if (data.getHenshinState() == HenshinState.IDLE) {
            proceedHenshin(player, Objects.requireNonNull(RiderConfig.findActiveDriverConfig(player)));
        } else if (data.getHenshinState() == HenshinState.TRANSFORMED) {
            proceedFormSwitch(player, pendingFormId);
        }

        // 重置状态
        data.setHenshinState(HenshinState.TRANSFORMED);
        data.setPendingFormId(null);

        // 同步状态
        if (player instanceof ServerPlayer serverPlayer) {
            HenshinSystem.syncHenshinState(serverPlayer);
        }
    }
}
