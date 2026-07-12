package com.jpigeon.ridebattlelib.server.system.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.api.RideBattleAPI;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.common.event.HenshinEvent;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.server.system.HenshinSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public class DriverActionManager {
    private static final DriverActionManager INSTANCE = new DriverActionManager();
    public static DriverActionManager getInstance() {
        return INSTANCE;
    }

    public void prepareHenshin(Player player, ResourceLocation formId) {
        if (RideBattleAPI.isTransformed(player)) return;
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("玩家 {} 进入变身缓冲阶段", player.getName().getString());
            RideBattleLib.LOGGER.debug("设置待处理形态: player={}, form={}", player.getName().getString(), formId);
        }

        // 触发变身事件
        HenshinEvent.Pre preHenshin = new HenshinEvent.Pre(player, config.getRiderId(), formId);
        NeoForge.EVENT_BUS.post(preHenshin);
        if (preHenshin.isCanceled()) {
            cancelHenshin(player);
        }
    }

    public void prepareFormSwitch(Player player, ResourceLocation oldFormId, ResourceLocation newFormId){
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (oldFormId.equals(newFormId)) return;
        if (config == null) return;

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("玩家 {} 进入形态缓冲阶段", player.getName().getString());
            RideBattleLib.LOGGER.debug("设置待处理形态: player={}, oldForm={}, form={}", player.getName().getString(), oldFormId, newFormId);
        }

        // 触发切换事件
        FormSwitchEvent.Pre preSwitch = new FormSwitchEvent.Pre(player, oldFormId, newFormId);
        NeoForge.EVENT_BUS.post(preSwitch);
        if (preSwitch.isCanceled()) {
            cancelHenshin(player);
        }
    }

    public void completeTransformation(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        ResourceLocation formId = data.getPendingFormId();
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("当前状态: {}, 形态ID: {}", data.getState(), formId);
        }

        if (formId == null) {
            RideBattleLib.LOGGER.error("尝试完成变身但目标形态丢失/不存在");
            return;
        }
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("完成变身序列: player={}, form={}", player.getName().getString(), formId);
        }

        if (!HenshinUtils.isTransformed(player)) {
            // 直接执行变身
            RiderConfig config = RiderConfig.findActiveDriverConfig(player);
            if (config != null) {
                HenshinSystem.getInstance().henshin(player, config.getRiderId());
            }
        } else {
            // 直接执行形态切换
            HenshinSystem.getInstance().switchForm(player, formId);
        }

        RideBattleLib.LOGGER.info("玩家{}变身为 {}", player.getName().getString(), formId);

        // 重置状态
        data.setState(HenshinState.TRANSFORMED);
        data.setPendingFormId(null);

        // 同步状态
        if (player instanceof ServerPlayer serverPlayer) {
            SyncManager.getInstance().syncHenshinState(serverPlayer);
        }
    }

    public void cancelHenshin(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        if (data.getState() == HenshinState.TRANSFORMING) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("取消玩家{}变身", player.getName());
            }
            data.setState(HenshinState.IDLE);
            data.setPendingFormId(null);

            if (player instanceof ServerPlayer serverPlayer) {
                SyncManager.getInstance().syncHenshinState(serverPlayer);
            }
        }
    }
}
