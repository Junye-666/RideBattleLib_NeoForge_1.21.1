package com.jpigeon.ridebattlelib.server.system;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.api.RideBattleAPI;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.FormMatchEngine;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.config.dynamic.DynamicFormCache;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.common.util.RiderUtils;
import com.jpigeon.ridebattlelib.server.event.*;
import com.jpigeon.ridebattlelib.server.system.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.server.system.helper.HenshinPhase;
import com.jpigeon.ridebattlelib.server.system.helper.SyncManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class HenshinSystem {
    private static final HenshinSystem INSTANCE = new HenshinSystem();

    public static HenshinSystem getInstance() {
        return INSTANCE;
    }

    private HenshinSystem() {
    }

    /**
     * 入口：driverAction —— 状态分发
     */
    public void driverAction(Player player) {
        if (player.level().isClientSide()) return;

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        switch (phaseOf(player)) {
            case IDLE, PAUSED -> doHenshin(player, config);   // PAUSED 下重按视为重新触发
            case TRANSFORMED -> doSwitch(player, config);
            case ACTIVATING, TRANSFORMING, SWITCHING, UNHENSHIN -> {
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug(
                            "driverAction 在非稳定状态 {} 被触发，忽略", phaseOf(player));
                }
            }
        }
    }

    /**
     * 执行变身
     *
     * @return 是否成功
     */
    private void doHenshin(Player player, RiderConfig config) {
        // 匹配目标形态
        Map<ResourceLocation, ItemStack> items = DriverSystem.getInstance().getDriverItems(player);
        ResourceLocation formId = FormMatchEngine.match(player, config, items);
        if (formId == null || formId.equals(RiderUtils.NULL)) return;

        FormConfig form = RiderRegistry.getForm(player, formId);
        if (form == null) {
            if (config.allowsDynamicForms()) {
                form = DynamicFormCache.getOrCreate(config, items);
                formId = form.getFormId();
            }
        }
        if (form == null) return;

        // 激活事件
        ItemStack driverItem = player.getItemBySlot(config.getDriverSlot());
        DriverActivationEvent activation = new DriverActivationEvent(player, driverItem);
        NeoForge.EVENT_BUS.post(activation);
        if (activation.isCanceled()) return;

        // 音效
        SoundEvent sound = form.getHenshinSound();
        if (sound != null) RideBattleAPI.playPublicSound(player, sound);

        // 进入中间态
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setPendingFormId(formId);
        if (data.getState() != HenshinState.TRANSFORMING) {
            data.setState(HenshinState.TRANSFORMING);
        }
        syncState(player);

        // 分派：pause / auto / immediate
        dispatchTransition(player, config, form, formId, null);
    }

    /**
     * 解除
     */
    public void unHenshin(Player player) {
        if (player.level().isClientSide()) return;

        HenshinPhase phase = phaseOf(player);
        if (phase != HenshinPhase.TRANSFORMED
                && phase != HenshinPhase.TRANSFORMING) return;

        HenshinSessionData data = HenshinUtils.getSessionData(player);
        if (data == null) return;

        RiderConfig config = RiderRegistry.getRider(data.riderId());
        if (config == null) return;

        UnhenshinEvent.Pre pre = new UnhenshinEvent.Pre(player, data);
        if (NeoForge.EVENT_BUS.post(pre).isCanceled()) return;

        config.getHenshinStrategy().unHenshin(player, data);

        transitionToState(player, HenshinState.IDLE, null);
        syncState(player);

        NeoForge.EVENT_BUS.post(new UnhenshinEvent.Post(player, data));
    }

    /**
     * 切换路径
     */
    private void doSwitch(Player player, RiderConfig config) {
        HenshinSessionData session = HenshinUtils.getSessionData(player);
        if (session == null) return;
        ResourceLocation oldFormId = session.formId();

        Map<ResourceLocation, ItemStack> items = DriverSystem.getInstance().getDriverItems(player);
        ResourceLocation newFormId = FormMatchEngine.match(player, config, items);
        if (newFormId == null || newFormId.equals(RiderUtils.NULL)) return;
        if (newFormId.equals(oldFormId)) return;

        FormConfig form = RiderRegistry.getForm(player, newFormId);
        if (form == null) {
            if (config.allowsDynamicForms()) {
                form = DynamicFormCache.getOrCreate(config, items);
                newFormId = form.getFormId();
            }
        }
        if (form == null) return;

        ItemStack driverItem = player.getItemBySlot(config.getDriverSlot());
        DriverActivationEvent activation = new DriverActivationEvent(player, driverItem);
        NeoForge.EVENT_BUS.post(activation);
        if (activation.isCanceled()) return;

        SoundEvent sound = form.getHenshinSound();
        if (sound != null) RideBattleAPI.playPublicSound(player, sound);

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setPendingFormId(newFormId);
        if (data.getState() != HenshinState.TRANSFORMING) {
            data.setState(HenshinState.TRANSFORMING);
        }
        syncState(player);

        dispatchTransition(player, config, form, newFormId, oldFormId);
    }

    /**
     * 三条分支：pause / auto / immediate
     */
    private void dispatchTransition(Player player, RiderConfig config,
                                    FormConfig form, ResourceLocation formId,
                                    @Nullable ResourceLocation oldFormId) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        boolean isSwitch = oldFormId != null;

        // shouldPause
        if (form.shouldPause()) {
            HenshinPauseEvent.Pre pre = new HenshinPauseEvent.Pre(player, config.getRiderId(), formId);
            NeoForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) {
                // 直接走完
                completeAndPostEvents(player, config, formId, oldFormId);
                return;
            }

            if (!isSwitch) {
                DriverActionManager.getInstance().prepareHenshin(player, formId);
            } else {
                DriverActionManager.getInstance().prepareFormSwitch(player, oldFormId, formId);
            }

            NeoForge.EVENT_BUS.post(new HenshinPauseEvent.Post(player, config.getRiderId(), formId));
            return;
        }

        // autoTicks > 0
        int autoTicks = form.getAutoCompleteTicks();
        if (autoTicks > 0) {
            if (!isSwitch) {
                DriverActionManager.getInstance().prepareHenshin(player, formId);
            } else {
                DriverActionManager.getInstance().prepareFormSwitch(player, oldFormId, formId);
            }
            // Pre 事件可能已取消 pendingFormId
            if (data.getPendingFormId() != null) {
                RideBattleAPI.scheduleTicks(autoTicks,
                        () -> DriverActionManager.getInstance().completeTransformation(player));
            }
            return;
        }

        // 分支 C：立即完成
        completeAndPostEvents(player, config, formId, oldFormId);
    }

    private void completeAndPostEvents(Player player, RiderConfig config,
                                       ResourceLocation formId,
                                       @Nullable ResourceLocation oldFormId) {
        boolean isSwitch = oldFormId != null;

        if (!isSwitch) {
            HenshinEvent.Pre pre = new HenshinEvent.Pre(player, config.getRiderId(), formId);
            NeoForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) {
                DriverActionManager.getInstance().cancelHenshin(player);
                return;
            }
        } else {
            FormSwitchEvent.Pre pre = new FormSwitchEvent.Pre(player, oldFormId, formId);
            NeoForge.EVENT_BUS.post(pre);
            if (pre.isCanceled()) {
                DriverActionManager.getInstance().cancelHenshin(player);
                return;
            }
        }
        DriverActionManager.getInstance().completeTransformation(player);
    }

    // 直接变身（跳过匹配，由外部 API 触发）
    public boolean henshin(Player player, ResourceLocation riderId) {
        if (player.level().isClientSide()) return false;

        // 守卫：已变身直接拒绝
        if (HenshinUtils.isTransformed(player)) return false;

        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return false;

        if (PenaltySystem.getInstance().isInCooldown(player)) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(
                        Component.literal("我的身体已经菠萝菠萝哒, 不能再变身了...")
                                .withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        Map<ResourceLocation, ItemStack> items = DriverSystem.getInstance().getDriverItems(player);
        if (!config.hasAuxDriverEquipped(player)) {
            items = new HashMap<>(items);
            items.keySet().removeAll(config.getAuxSlotDefinitions().keySet());
        }

        ResourceLocation formId = FormMatchEngine.match(player, config, items);
        if (formId == null || formId.equals(RiderUtils.NULL)) return false;

        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null && config.allowsDynamicForms()) {
            form = DynamicFormCache.getOrCreate(config, items);
            formId = form.getFormId();
        }

        config.getHenshinStrategy().performHenshin(player, config, formId);
        transitionToState(player, HenshinState.TRANSFORMED, formId);
        syncState(player);

        NeoForge.EVENT_BUS.post(new HenshinEvent.Post(player, riderId, formId));
        return true;
    }

    // 直接切形态
    public void switchForm(Player player, ResourceLocation newFormId) {
        if (player.level().isClientSide()) return;

        if (!HenshinUtils.isTransformed(player)) return;

        if (newFormId == null) {
            unHenshin(player);
            return;
        }

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        HenshinSessionData session = HenshinUtils.getSessionData(player);
        if (session == null) return;
        ResourceLocation oldFormId = session.formId();
        if (newFormId.equals(oldFormId)) return;

        config.getHenshinStrategy().performFormSwitch(player, session, newFormId);

        FormSwitchEvent.Post post = new FormSwitchEvent.Post(player, oldFormId, newFormId);
        NeoForge.EVENT_BUS.post(post);

        syncState(player);
    }

    //====================检查方法====================
    // 相位计算
    private HenshinPhase phaseOf(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        boolean transformed = data.isTransformed();
        HenshinState state = data.getState();

        if (transformed) {
            return state == HenshinState.TRANSFORMING
                    ? HenshinPhase.TRANSFORMING
                    : HenshinPhase.TRANSFORMED;
        } else {
            if (state == HenshinState.TRANSFORMING) {
                // 有 pendingFormId 说明在 ACTIVATING/PAUSED
                return data.getPendingFormId() != null
                        ? HenshinPhase.PAUSED
                        : HenshinPhase.ACTIVATING;
            }
            return HenshinPhase.IDLE;
        }
    }

    // 状态转移
    private void transitionToState(Player player, HenshinState state,
                                   @Nullable ResourceLocation formId) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setState(state);
        data.setPendingFormId(formId);
        if (state != HenshinState.TRANSFORMED) {
            data.endHenshinSession();
        }
        syncState(player);
    }

    private void syncState(Player player) {
        if (player instanceof ServerPlayer sp) {
            SyncManager.getInstance().syncHenshinState(sp);
            SyncManager.getInstance().syncDriverData(sp);
        }
    }
}
