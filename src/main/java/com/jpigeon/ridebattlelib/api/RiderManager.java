package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.form.DynamicFormConfig;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.CountdownManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.SyncManager;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.*;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import com.jpigeon.ridebattlelib.core.system.skill.SkillSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 假面骑士系统快捷方法管理器。
 * <p>
 * 提供静态方法供其他模组调用，如变身、形态切换、物品管理等。
 * <p>
 * 所有方法均线程安全，可在客户端或服务端调用。
 */
public final class RiderManager {
    private RiderManager() {}
    // ================ 变身系统快捷方法 ================

    /**
     * 尝试让玩家变身。
     *
     * @param player 玩家
     * @return 是否成功发起变身
     */
    public static boolean transform(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config != null) {
            PacketHandler.sendToServer(new HenshinPacket(player.getUUID(), config.getRiderId()));
            return true;
        }
        return false;
    }

    /**
     * 解除让玩家变身。
     *
     * @param player 玩家
     * @return 是否成功解除变身
     */
    public static boolean unTransform(Player player) {
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            PacketHandler.sendToServer(new UnhenshinPacket(player.getUUID()));
            return true;
        }
        return false;
    }

    /**
     * 尝试切换玩家形态。
     *
     * @param player 玩家
     * @return 是否成功切换
     */
    public static boolean switchForm(Player player, ResourceLocation newFormId) {
        if (!HenshinSystem.INSTANCE.isTransformed(player)) {
            PacketHandler.sendToServer(new SwitchFormPacket(player.getUUID(), newFormId));
            return true;
        }
        return false;
    }

    /**
     * 快捷完成变身序列
     */
    public static void completeHenshin(Player player) {
        DriverActionManager.INSTANCE.completeTransformation(player);
    }

    // ================驱动器系统快捷方法 ================

    /**
     * 插入物品至驱动器
     */
    public static boolean insertItemToSlot(Player player, ResourceLocation slotId, ItemStack stack) {
        if (player.level().isClientSide) {
            // 客户端发送网络包
            PacketHandler.sendToServer(new InsertItemPacket(player.getUUID(), slotId, stack));
            return true; // 假设成功，实际结果需要服务端确认
        } else {
            // 服务端直接调用
            return DriverSystem.INSTANCE.insertItem(player, slotId, stack);
        }
    }

    /**
     * 将单个物品从驱动器中取出
     */
    public static void extractItemFromSlot(Player player, ResourceLocation slotId) {
        PacketHandler.sendToServer(new ExtractItemPacket(player.getUUID(), slotId));
    }

    /**
     * 为玩家返还所有驱动器物品
     */
    public static void returnDriverItems(Player player) {
        PacketHandler.sendToServer(new ReturnItemsPacket());
    }

    // ================ 吃瘪系统快捷方法 ================

    /**
     * 强制解除变身
     */
    public static void penaltyUntransform(Player player) {
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            PenaltySystem.PENALTY_SYSTEM.penaltyUnhenshin(player);
        }
    }

    /**
     * 开始变身冷却
     */
    public static void applyCooldown(Player player, int seconds) {
        PenaltySystem.PENALTY_SYSTEM.startCooldown(player, seconds);
    }

    /**
     * 检查是否在冷却中
     */
    public static boolean isInCooldown(Player player) {
        return PenaltySystem.PENALTY_SYSTEM.isInCooldown(player);
    }

    // ================ 技能系统快捷方法 ================

    /**
     * 触发技能
     */
    public static boolean triggerSkill(Player player, ResourceLocation formId, ResourceLocation skillId) {
        return SkillSystem.triggerSkillEvent(player, formId, skillId);
    }

    /**
     * 获取玩家当前形态的技能列表
     * @param player 玩家
     * @return 技能ID列表，无技能则返回空列表
     */
    public static List<ResourceLocation> getCurrentFormSkills(Player player) {
        FormConfig formConfig = getActiveFormConfig(player);
        return formConfig != null ? formConfig.getSkillIds() : Collections.emptyList();
    }

    // ================ 快速获取 ================

    /**
     * 获取玩家当前骑士配置
     * @param player 玩家
     * @return 当前骑士配置，未找到则返回null
     */
    @Nullable
    public static RiderConfig getActiveRiderConfig(Player player) {
        return RiderConfig.findActiveDriverConfig(player);
    }

    /**
     * 获取玩家当前激活的形态配置
     * @param player 玩家
     * @return 当前形态配置，未找到则返回null
     */
    @Nullable
    public static FormConfig getActiveFormConfig(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return null;
        return config.getActiveFormConfig(player);
    }

    /**
     * 获取当前变身数据
     * @param player 玩家
     * @return 玩家当前形态Id
     */
    @Nullable
    public static ResourceLocation getCurrentForm(Player player) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        return data != null ? data.formId() : null;
    }

    /**
     * 通过形态Id获取形态配置
     * @param formId 形态Id
     * @return 通过Id匹配的配置
     */
    @Nullable
    public static FormConfig getFormConfig(ResourceLocation formId) {
        // 先检查预设形态
        FormConfig form = RiderRegistry.getForm(formId);
        if (form == null) {
            // 再检查动态形态
            form = DynamicFormConfig.getDynamicForm(formId);
        }
        return form;
    }

    /**
     * 获取玩家当前目标形态
     * @return 当前缓冲形态Id
     */
    @Nullable
    public static ResourceLocation getPendingForm(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        return data.getPendingFormId();
    }


    /**
     * 获取玩家驱动器内物品
     * @return 玩家当前驱动器内物品列表
     */
    public static Map<ResourceLocation, ItemStack> getDriverItems(Player player) {
        return DriverSystem.INSTANCE.getDriverItems(player);
    }


    /**
     * 获取当前选中的技能ID
     */
    @Nullable
    public static ResourceLocation getCurrentSkill(Player player) {
        if (!isTransformed(player)) return null;

        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return null;

        FormConfig form = RiderRegistry.getForm(data.formId());
        if (form == null) return null;

        return form.getCurrentSkillId(player);
    }

    // 快速检查方法

    /**
     * 检查玩家是否为特定骑士
     */
    public static boolean isSpecificRider(Player player, ResourceLocation riderId) {
        RiderConfig config = getActiveRiderConfig(player);
        return config != null && config.getRiderId().equals(riderId);
    }

    /**
     * 检查玩家是否拥有特定形态
     * @param player 玩家
     * @param formId 形态ID
     * @return 是否拥有该形态
     */
    public static boolean isSpecificForm(Player player, ResourceLocation formId) {
        ResourceLocation currentForm = getCurrentForm(player);
        return currentForm != null && currentForm.equals(formId);
    }

    /**
     * 快捷检查变身状态
     */
    public static boolean isTransformed(Player player) {
        return HenshinSystem.INSTANCE.isTransformed(player);
    }

    /**
     * 检查玩家是否处于变身过程中
     */
    public static boolean isTransforming(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        return data.getHenshinState() == HenshinState.TRANSFORMING;
    }

    /**
     * 检查玩家是否装备了驱动器
     */
    public static boolean hasDriverEquipped(Player player) {
        return RiderConfig.findActiveDriverConfig(player) != null;
    }

    /**
     * 检查玩家是否装备了特定骑士的驱动器
     */
    public static boolean hasSpecificDriverEquipped(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        return config != null && config.getRiderId().equals(riderId);
    }

    /**
     * 检查玩家是否装备了辅助驱动器
     */
    public static boolean hasAuxDriverEquipped(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        return config != null && config.hasAuxDriverEquipped(player);
    }

    /**
     * 检查玩家是否可以变身（满足所有条件）
     */
    public static boolean canTransform(Player player) {
        return hasDriverEquipped(player) &&
                !isInCooldown(player) &&
                !isTransformed(player);
    }

    // ================ 数据同步方法 ================

    /**
     * 强制刷新所有状态同步
     */
    public static void syncClientState(ServerPlayer player) {
        SyncManager.INSTANCE.syncAllPlayerData(player);
    }

    /**
     * 同步驱动器数据
     */
    public static void syncDriverData(ServerPlayer player) {
        SyncManager.INSTANCE.syncDriverData(player);
    }

    /**
     * 同步变身状态
     */
    public static void syncHenshinState(ServerPlayer player) {
        SyncManager.INSTANCE.syncHenshinState(player);
    }

    // ================ 开发便捷方法 ================

    /**
     * 播放公共音效 - 所有附近玩家都能听到
     * 现在只在服务端调用，因为所有逻辑都在服务端执行
     */
    public static void playPublicSound(Player player, SoundEvent sound, SoundSource category, float volume, float pitch) {
        if (!player.level().isClientSide) {
            // 服务端：广播给所有玩家
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, category, volume, pitch);
        }
    }

    /**
     * 播放公共音效（简化单个参数）
     */
    public static void playPublicSound(Player player, SoundEvent sound, float volume) {
        playPublicSound(player, sound, SoundSource.PLAYERS, volume, 1.0F);
    }

    /**
     * 播放公共音效（简化两个参数）
     */
    public static void playPublicSound(Player player, SoundEvent sound) {
        playPublicSound(player, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 倒计时方法
     * @param ticks 等待游戏刻数
     * @param callback 执行任务
     */
    public static void scheduleTicks(int ticks, Runnable callback){
        CountdownManager.getInstance().scheduleTask(ticks, callback);
    }

    /**
     * 倒计时方法
     * @param seconds 等待秒数
     * @param callback 执行任务
     */
    public static void scheduleSeconds(float seconds, Runnable callback){
        scheduleTicks((int) (seconds * 20), callback);
    }

    /**
     * 重置玩家所有变身相关状态（谨慎使用）
     */
    public static void resetPlayerState(Player player) {
        if (isTransformed(player)) {
            unTransform(player);
        }

        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setHenshinState(HenshinState.IDLE);
        data.setPendingFormId(null);
        data.setPenaltyCooldownEnd(0);
        data.setCurrentSkillIndex(0);

        player.removeTag("penalty_cooldown");
        player.removeTag("just_respawned");
    }
}
