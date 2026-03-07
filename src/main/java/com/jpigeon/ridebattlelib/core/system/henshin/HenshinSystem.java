package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.HenshinContext;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.event.*;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.data.TransformedData;
import com.jpigeon.ridebattlelib.core.system.network.packet.SyncHenshinStatePacket;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HenshinSystem implements IHenshinSystem {
    private static final HenshinSystem INSTANCE = new HenshinSystem();
    public static HenshinSystem getInstance() {
        return INSTANCE;
    }

    public static final Map<UUID, Boolean> CLIENT_TRANSFORMED_CACHE = new ConcurrentHashMap<>();

    @Override
    public void driverAction(Player player) {
        if (player.level().isClientSide) {
            RideBattleLib.LOGGER.warn("driverAction 在客户端调用，应该通过数据包触发");
            return;
        }
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.getInstance().getDriverItems(player);
        ResourceLocation formId = config.matchForm(player, driverItems);
        if (formId == null) return;
        FormConfig formConfig = config.getActiveFormConfig(player);
        if (formConfig == null) return;
        ItemStack driverItem = player.getItemBySlot(config.getDriverSlot());

        DriverActivationEvent driverEvent = new DriverActivationEvent(player, driverItem);
        NeoForge.EVENT_BUS.post(driverEvent);
        if (driverEvent.isCanceled()) return;

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setPendingFormId(formId);
        if (data.getHenshinState() != HenshinState.TRANSFORMING) {
            data.setHenshinState(HenshinState.TRANSFORMING);
        }
        // 同步状态
        if (player.level().isClientSide) {
            // 客户端发送同步请求
            PacketDistributor.sendToServer(new SyncHenshinStatePacket(
                    HenshinState.TRANSFORMING,
                    formId
            ));
        } else if (player instanceof ServerPlayer serverPlayer) {
            // 服务端直接同步
            HenshinContext.DATA_SYNC.syncHenshinState(serverPlayer);
        }

        TransformedData oldData = getTransformedData(player);
        ResourceLocation oldFormId = oldData != null ? oldData.formId() : null;

        // 处理变身逻辑
        if (formConfig.shouldPause()) {
            // 需要暂停的变身流程
            HenshinPauseEvent.Pre prePause = new HenshinPauseEvent.Pre(player, config.getRiderId(), formId);
            if (NeoForge.EVENT_BUS.post(prePause).isCanceled()) {
                completeAndSendEvents(player, config, formId, oldFormId);
                return;
            }

            if (!isTransformed(player)) {
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
        if (!isTransformed(player)) {
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

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) return false;

        Map<ResourceLocation, ItemStack> driverItems = DriverSystem.getInstance().getDriverItems(player);

        // 如果没有装备辅助驱动器，则移除所有辅助槽位
        if (!config.hasAuxDriverEquipped(player)) {
            driverItems = new HashMap<>(driverItems);
            driverItems.keySet().removeAll(config.getAuxSlotDefinitions().keySet());
        }

        ResourceLocation formId = config.matchForm(player, driverItems);
        if (!canHenshin(player) || formId == null) return false;

        FormConfig formConfig = RiderRegistry.getForm(formId);

        // 如果是动态形态
        if (formConfig == null) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("形态 {} 未注册，尝试作为动态形态处理", formId);
            }
            formConfig = DynamicFormConfig.getOrCreateDynamicForm(
                    player, config, driverItems
            );

            // 确保formId一致性
            if (!formConfig.getFormId().equals(formId)) {
                RideBattleLib.LOGGER.warn("动态形态ID不一致: 预期={}, 实际={}",
                        formId, formConfig.getFormId());
                formId = formConfig.getFormId();
            }
        }

        // 执行变身
        config.getHenshinStrategy().performHenshin(player, config, formId);

        transitionToState(player, HenshinState.TRANSFORMED, formId);

        if (player instanceof ServerPlayer serverPlayer) {
            HenshinContext.DATA_SYNC.syncTransformedState(serverPlayer);
        }

        // 触发变身回调事件
        HenshinEvent.Post postHenshin = new HenshinEvent.Post(player, riderId, formId);
        NeoForge.EVENT_BUS.post(postHenshin);

        return true;
    }

    @Override
    public void unHenshin(Player player) {
        TransformedData data = getTransformedData(player);

        if (data != null) {
            RiderConfig config = data.config();
            // 触发 Pre 事件（可取消）
            UnhenshinEvent.Pre preUnHenshin = new UnhenshinEvent.Pre(player, data);
            if (NeoForge.EVENT_BUS.post(preUnHenshin).isCanceled()) return;

            // 调用策略执行解除
            config.getHenshinStrategy().unHenshin(player, data);
            HenshinSystem.INSTANCE.transitionToState(player, HenshinState.IDLE, null);

            // 触发 Post 事件
            NeoForge.EVENT_BUS.post(new UnhenshinEvent.Post(player, data));
        }
    }

    @Override
    public void switchForm(Player player, ResourceLocation newFormId) {
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

        TransformedData data = getTransformedData(player);
        if (data == null) {
            RideBattleLib.LOGGER.error("无法获取变身数据");
            return;
        }
        ResourceLocation oldFormId = data.formId();

        config.getHenshinStrategy().performFormSwitch(player, newFormId);

        // 触发形态切换事件
        if (!newFormId.equals(oldFormId)) {
            FormSwitchEvent.Post postFormSwitch = new FormSwitchEvent.Post(player, oldFormId, newFormId);
            NeoForge.EVENT_BUS.post(postFormSwitch);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            HenshinContext.DATA_SYNC.syncTransformedState(serverPlayer);
        }
    }

    //====================检查方法====================

    @Override
    public boolean isTransformed(Player player) {
        // 客户端检查缓存，服务端检查真实数据
        if (player.level().isClientSide) {
            return CLIENT_TRANSFORMED_CACHE.getOrDefault(player.getUUID(), false);
        }
        return player.getData(RiderAttachments.RIDER_DATA).getTransformedData() != null;
    }

    private boolean canHenshin(Player player) {
        if (PenaltySystem.PENALTY_SYSTEM.isInCooldown(player)) {
            if (player instanceof ServerPlayer) {
                player.displayClientMessage(Component.literal("我的身体已经菠萝菠萝哒, 不能再变身了...").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        return true;
    }

    public void transitionToState(Player player, HenshinState state, @Nullable ResourceLocation formId) {
        RiderData oldData = player.getData(RiderAttachments.RIDER_DATA);

        // 创建新数据副本
        RiderData newData = new RiderData(
                new HashMap<>(oldData.mainDriverItems),
                new HashMap<>(oldData.auxDriverItems),
                oldData.getTransformedData(),  // 保留现有变身数据
                state,                         // 新状态
                formId,                        // 新待处理形态
                oldData.getPenaltyCooldownEnd(),
                0
        );

        // 保存更新后的数据
        player.setData(RiderAttachments.RIDER_DATA, newData);

        if (player instanceof ServerPlayer serverPlayer) {
            HenshinContext.DATA_SYNC.syncHenshinState(serverPlayer);
        }

        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("状态变更: {} -> {} (形态: {})",
                    oldData.getHenshinState(), state, formId);
        }
    }

    //====================Getter方法====================
    @Override
    @Nullable
    public TransformedData getTransformedData(Player player) {
        TransformedAttachmentData attachmentData = player.getData(RiderAttachments.RIDER_DATA).getTransformedData();
        if (attachmentData == null) return null;

        RiderConfig config = RiderRegistry.getRider(attachmentData.riderId());
        if (config == null) return null;

        return new TransformedData(
                config,
                attachmentData.formId(),
                attachmentData.originalGear(),
                attachmentData.driverSnapshot()
        );
    }
}
