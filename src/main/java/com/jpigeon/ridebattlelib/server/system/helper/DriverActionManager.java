package com.jpigeon.ridebattlelib.server.system.helper;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.server.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.server.event.HenshinEvent;
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

    private DriverActionManager() {
    }

    /**
     * 已在 driverAction 里设过 pendingFormId 和 TRANSFORMING，这里只发 Pre 事件
     */
    public void prepareHenshin(Player player, ResourceLocation formId) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        HenshinEvent.Pre pre = new HenshinEvent.Pre(player, config.getRiderId(), formId);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) {
            cancelHenshin(player);
        }
    }

    public void prepareFormSwitch(Player player, ResourceLocation oldFormId, ResourceLocation newFormId) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        FormSwitchEvent.Pre pre = new FormSwitchEvent.Pre(player, oldFormId, newFormId);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) {
            cancelHenshin(player);
        }
    }

    /**
     * completeTransformation 保留原逻辑（从 pendingFormId 恢复意图并执行）
     */
    public void completeTransformation(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        ResourceLocation formId = data.getPendingFormId();
        if (formId == null) {
            // 由于 doHenshin 里已经检查过 pendingFormId != null，
            // 这里的 null 只可能来自 cancelHenshin，不打 ERROR
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("completeTransformation 被丢弃：pendingFormId 为空");
            }
            return;
        }

        boolean alreadyTransformed = HenshinUtils.isTransformed(player);

        if (!alreadyTransformed) {
            RiderConfig config = RiderConfig.findActiveDriverConfig(player);
            if (config != null) {
                HenshinSystem.getInstance().henshin(player, config.getRiderId());
            }
        } else {
            HenshinSystem.getInstance().switchForm(player, formId);
        }

        data.setState(HenshinState.TRANSFORMED);
        data.setPendingFormId(null);

        if (player instanceof ServerPlayer sp) {
            SyncManager.getInstance().syncHenshinState(sp);
        }
    }

    public void cancelHenshin(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        if (data.getState() == HenshinState.TRANSFORMING) {
            data.setState(HenshinState.IDLE);
            data.setPendingFormId(null);
            if (player instanceof ServerPlayer sp) {
                SyncManager.getInstance().syncHenshinState(sp);
            }
        }
    }
}