package com.jpigeon.ridebattlelib.server.system;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.config.DynamicFormConfig;
import com.jpigeon.ridebattlelib.common.config.FormConfig;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.HenshinSessionData;
import com.jpigeon.ridebattlelib.common.data.HenshinState;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.event.*;
import com.jpigeon.ridebattlelib.common.registry.RiderRegistry;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.common.util.RiderUtils;
import com.jpigeon.ridebattlelib.server.system.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.server.system.helper.SyncManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HenshinSystem {
    private static final HenshinSystem INSTANCE = new HenshinSystem();
    public static HenshinSystem getInstance() {
        return INSTANCE;
    }

    public static final Map<UUID, Boolean> CLIENT_TRANSFORMED_CACHE = new ConcurrentHashMap<>();

    public void driverAction(Player player) {
        if (player.level().isClientSide()) {
            RideBattleLib.LOGGER.warn("driverAction 在客户端调用，应该通过数据包触发");
            return;
        }
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.getInstance().getDriverItems(player);
        ResourceLocation formId = config.matchForm(player, driverItems);
        if (formId == null || formId.equals(RiderUtils.NULL)) return;
        FormConfig formConfig = config.getActiveFormConfig(player);
        if (formConfig == null) return;
        ItemStack driverItem = player.getItemBySlot(config.getDriverSlot());

        DriverActivationEvent driverEvent = new DriverActivationEvent(player, driverItem);
        NeoForge.EVENT_BUS.post(driverEvent);
        if (driverEvent.isCanceled()) return;

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setPendingFormId(formId);
        if (data.getState() != HenshinState.TRANSFORMING) {
            data.setState(HenshinState.TRANSFORMING);
        }
        // 同步状态
        syncState(player);

        HenshinSessionData oldData = HenshinUtils.getSessionData(player);
        ResourceLocation oldFormId = oldData != null ? oldData.formId() : null;

        // 处理变身逻辑
        if (formConfig.shouldPause()) {
            // 需要暂停的变身流程
            HenshinPauseEvent.Pre prePause = new HenshinPauseEvent.Pre(player, config.getRiderId(), formId);
            if (NeoForge.EVENT_BUS.post(prePause).isCanceled()) {
                completeAndSendEvents(player, config, formId, oldFormId);
                return;
            }

            if (!HenshinUtils.isTransformed(player)) {
                DriverActionManager.getInstance().prepareHenshin(player, formId);
            } else if (oldFormId != null) {
                DriverActionManager.getInstance().prepareFormSwitch(player, oldFormId, formId);
            }

            HenshinPauseEvent.Post postPause = new HenshinPauseEvent.Post(player, config.getRiderId(), formId);
            NeoForge.EVENT_BUS.post(postPause);
        } else {
            completeAndSendEvents(player, config, formId, oldFormId);
        }
    }

    private void completeAndSendEvents(Player player, RiderConfig config, ResourceLocation formId, ResourceLocation oldFormId) {
        if (!HenshinUtils.isTransformed(player)) {
            HenshinEvent.Pre preHenshin = new HenshinEvent.Pre(player, config.getRiderId(), formId);
            NeoForge.EVENT_BUS.post(preHenshin);
            if (preHenshin.isCanceled()) {
                DriverActionManager.getInstance().cancelHenshin(player);
            }
        } else {
            FormSwitchEvent.Pre preSwitch = new FormSwitchEvent.Pre(player, oldFormId, formId);
            NeoForge.EVENT_BUS.post(preSwitch);
            if (preSwitch.isCanceled()) {
                DriverActionManager.getInstance().cancelHenshin(player);
            }
        }
        DriverActionManager.getInstance().completeTransformation(player);
    }

    /**
     * 执行变身
     *
     * @return 是否成功
     */
    public boolean henshin(Player player, ResourceLocation riderId) {
        if (player.level().isClientSide()) return false;
        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return false;

        if (PenaltySystem.getInstance().isInCooldown(player)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.literal("我的身体已经菠萝菠萝哒, 不能再变身了...").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.getInstance().getDriverItems(player);

        if (!config.hasAuxDriverEquipped(player)) {
            // 过滤辅助槽位
            driverItems = new HashMap<>(driverItems);
            driverItems.keySet().removeAll(config.getAuxSlotDefinitions().keySet());
        }

        ResourceLocation formId = config.matchForm(player, driverItems);
        if (formId == null || formId.equals(RiderUtils.NULL)) return false;

        FormConfig formConfig = RiderRegistry.getForm(formId);
        if (formConfig == null) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("形态 {} 未注册，尝试作为动态形态处理", formId);
            }
            // 关键修复：如果骑士允许动态形态，则强制创建/获取动态形态
            if (config.allowsDynamicForms()) {
                formConfig = DynamicFormConfig.getOrCreateDynamicForm(config, driverItems);
                // 使用实际生成的形态ID
                formId = formConfig.getFormId();
            }
        }

        // 执行变身
        config.getHenshinStrategy().performHenshin(player, config, formId);

        transitionToState(player, HenshinState.TRANSFORMED, formId);
        syncState(player);

        // 触发变身回调事件
        HenshinEvent.Post postHenshin = new HenshinEvent.Post(player, riderId, formId);
        NeoForge.EVENT_BUS.post(postHenshin);

        return true;
    }

    public void unHenshin(Player player) {
        if (player.level().isClientSide()) return;
        HenshinSessionData data = HenshinUtils.getSessionData(player);

        if (data != null) {
            RiderConfig config = RiderRegistry.getRider(data.riderId());
            // 触发 Pre 事件（可取消）
            UnhenshinEvent.Pre preUnHenshin = new UnhenshinEvent.Pre(player, data);
            if (NeoForge.EVENT_BUS.post(preUnHenshin).isCanceled()) return;

            // 调用策略执行解除
            config.getHenshinStrategy().unHenshin(player, data);

            transitionToState(player, HenshinState.IDLE, null);
            syncState(player);

            // 触发 Post 事件
            NeoForge.EVENT_BUS.post(new UnhenshinEvent.Post(player, data));
        }
    }

    public void switchForm(Player player, ResourceLocation newFormId) {
        if (player.level().isClientSide()) return;

        // 如果新形态ID为null，表示无法匹配形态
        if (newFormId == null) {
            unHenshin(player);
            return;
        }

        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;

        // 确保只在装备了辅助驱动器时才匹配辅助槽位
        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.getInstance().getDriverItems(player);
        if (!config.hasAuxDriverEquipped(player)) {
            // 过滤掉辅助槽位
            driverItems = new HashMap<>(driverItems);
            driverItems.keySet().removeAll(config.getAuxSlotDefinitions().keySet());
        }

        HenshinSessionData data = HenshinUtils.getSessionData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }
        ResourceLocation oldFormId = data.formId();
        config.getHenshinStrategy().performFormSwitch(player, data, newFormId);

        // 触发形态切换事件
        if (!newFormId.equals(oldFormId)) {
            FormSwitchEvent.Post postFormSwitch = new FormSwitchEvent.Post(player, oldFormId, newFormId);
            NeoForge.EVENT_BUS.post(postFormSwitch);
        }

        syncState(player);
    }

    //====================检查方法====================

    public void transitionToState(Player player, HenshinState state, @Nullable ResourceLocation formId) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setState(state);
        data.setPendingFormId(formId);
        if (state != HenshinState.TRANSFORMED) {
            // 非变身状态清空会话
            data.endHenshinSession();
        }
        syncState(player);
    }

    /**
     * 同步状态到客户端
     */
    private void syncState(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            SyncManager.getInstance().syncHenshinState(serverPlayer);
            SyncManager.getInstance().syncDriverData(serverPlayer);
        }
    }
}
