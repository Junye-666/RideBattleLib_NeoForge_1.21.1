package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderData;
import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.event.SkillEvent;
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
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

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
     * @param player 玩家
     * @return 是否成功发起变身
     */
    public static boolean transform(Player player) {
        if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("尝试为玩家{}变身", player.getName().getString());
        PacketHandler.sendToServer(new DriverActionPacket(player.getUUID()));
        return true;
    }

    /**
     * 解除玩家变身。
     * @param player 玩家
     * @return 是否成功解除变身
     */
    public static boolean unTransform(Player player) {
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("尝试解除玩家{}变身", player.getName().getString());
            PacketHandler.sendToServer(new UnhenshinPacket(player.getUUID()));
            return true;
        }
        return false;
    }

    /**
     * 尝试切换玩家形态。
     * @param player 玩家
     * @return 是否成功切换
     */
    public static boolean switchForm(Player player, ResourceLocation newFormId) {
        if (isTransformed(player) && getCurrentFormId(player) != newFormId) {
            if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("尝试切换玩家{}形态{}", player.getName().getString(), newFormId);
            PacketHandler.sendToServer(new SwitchFormPacket(player.getUUID(), newFormId));
            return true;
        }
        return false;
    }

    /**
     * 快捷完成变身序列
     */
    public static void completeHenshin(Player player) {
        if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("完成玩家{}变身序列", player.getName().getString());
        DriverActionManager.INSTANCE.completeTransformation(player);
    }

    // ================驱动器系统快捷方法 ================

    /**
     * 插入物品至驱动器
     * 注意：此方法不触发AUTO变身
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
     * 快捷方法
     */
    public static boolean insertItemToSlot(Player player, ResourceLocation slotId, Item item){
        return insertItemToSlot(player, slotId, item.getDefaultInstance());
    }

    /**
     * 将单个物品从驱动器中取出
     */
    public static void extractItemFromSlot(Player player, ResourceLocation slotId) {
        if (getItemForSlot(player, slotId).isEmpty()) return;
        if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("为玩家{}从槽位{}取出物品", player.getName().getString(), slotId);
        PacketHandler.sendToServer(new ExtractItemPacket(player.getUUID(), slotId));
    }

    /**
     * 为玩家返还所有驱动器物品
     */
    public static void returnDriverItems(Player player) {
        if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("返还玩家驱动器物品{}", player.getName().getString());
        PacketHandler.sendToServer(new ReturnItemsPacket());
    }

    // ================ 吃瘪系统快捷方法 ================

    /**
     * 强制解除变身
     */
    public static void penaltyUntransform(Player player) {
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("强制解除玩家{}变身", player.getName().getString());
            PenaltySystem.PENALTY_SYSTEM.penaltyUnhenshin(player);
        }
    }

    /**
     * 开始变身冷却
     */
    public static void applyCooldown(Player player, int seconds) {
        if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("设置玩家{}变身冷却{}秒", player.getName().getString(), seconds);
        PenaltySystem.PENALTY_SYSTEM.startCooldown(player, seconds);
    }

    /**
     * 检查是否在冷却中
     */
    public static boolean isInCooldown(Player player) {
        boolean inCooldown = PenaltySystem.PENALTY_SYSTEM.isInCooldown(player);
        if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("玩家{}是否处于冷却状态：{}", player.getName().getString(), inCooldown);
        return inCooldown;
    }

    // ================ 技能系统快捷方法 ================

    /**
     * 触发技能
     */
    public static boolean triggerSkill(Player player, ResourceLocation formId, ResourceLocation skillId, SkillEvent.SkillTriggerType type) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        if (data == null) return false;

        if (skillId != null) {
            // 检查技能冷却
            if (SkillSystem.isSkillOnCooldown(player, skillId)) {
                int remaining = SkillSystem.getSkillRemainingCooldown(player, skillId);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(
                            Component.literal("技能冷却中，剩余时间: " + remaining + "秒")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                return false;
            }

            // 只触发事件，不执行具体逻辑
            if (SkillSystem.triggerSkillEvent(player, formId, skillId, type)) {
                // 技能成功触发后开始冷却
                SkillSystem.startSkillCooldown(player, skillId);
                return true;
            }
        }
        return false;
    }

    public static boolean triggerSkill(Player player, ResourceLocation formId, ResourceLocation skillId) {
        return triggerSkill(player, formId, skillId, SkillEvent.SkillTriggerType.OTHER);
    }

    public static boolean triggerSkill(Player player, ResourceLocation skillId) {
        return triggerSkill(player, getCurrentFormId(player), SkillEvent.SkillTriggerType.OTHER);
    }

    public static boolean triggerSkill(Player player, ResourceLocation skillId, SkillEvent.SkillTriggerType type) {
        return triggerSkill(player, getCurrentFormId(player), skillId, type);
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
    public static ResourceLocation getCurrentFormId(Player player) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        return data != null ? data.formId() : null;
    }

    /**
     * 通过形态Id获取形态配置
     * @param formId 形态Id
     * @return 通过Id匹配的配置
     */
    @Nullable
    public static FormConfig getFormConfig(Player player, ResourceLocation formId) {
        // 优先从玩家当前骑士获取
        FormConfig form = RiderRegistry.getForm(player, formId);

        if (form == null) {
            // 回退到动态形态
            form = DynamicFormConfig.getDynamicForm(formId);
        }

        if (form == null && Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("未找到形态配置: {} (玩家: {})", formId,
                    player.getName().getString());
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

        FormConfig form = getFormConfig(player, data.formId());
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
     * 检查玩家是否变身为特定形态
     * @param player 玩家
     * @param formId 形态ID
     * @return 是否变身为该形态
     */
    public static boolean isSpecificForm(Player player, ResourceLocation formId) {
        ResourceLocation currentForm = getCurrentFormId(player);
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

    /**
     * 检查玩家驱动器中是否有指定物品（任何槽位）
     *
     * @param player 玩家
     * @param item 要检查的物品
     * @return 当驱动器中存在该物品返回true
     */
    public static boolean hasItemInDriver(Player player, Item item) {
        if (player == null || item == null) return false;

        Map<ResourceLocation, ItemStack> driverItems = getDriverItems(player);

        for (ItemStack stack : driverItems.values()) {
            if (!stack.isEmpty() && stack.is(item)) {
                if (Config.DEVELOPER_MODE.get()) {
                    RideBattleLib.LOGGER.debug("在驱动器中找到物品: {}",
                            BuiltInRegistries.ITEM.getKey(item));
                }
                return true;
            }
        }

        if (Config.DEVELOPER_MODE.get()) {
            RideBattleLib.LOGGER.debug("驱动器中未找到物品: {}", BuiltInRegistries.ITEM.getKey(item));
        }
        return false;
    }

    /**
     * 检查玩家驱动器中是否有指定物品（考虑组件匹配）
     *
     * @param player 玩家
     * @param itemStack 要检查的物品堆栈（包含组件）
     * @return 当驱动器中存在匹配的物品返回true
     */
    public static boolean hasItemInDriver(Player player, ItemStack itemStack) {
        if (player == null || itemStack.isEmpty()) return false;

        Map<ResourceLocation, ItemStack> driverItems = getDriverItems(player);

        for (ItemStack stack : driverItems.values()) {
            if (ItemStack.isSameItemSameComponents(stack, itemStack)) {
                if (Config.DEVELOPER_MODE.get()) {
                    RideBattleLib.LOGGER.debug("在驱动器中找到匹配物品: {} (组件匹配)",
                            BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家驱动器的指定槽位中是否有指定物品
     *
     * @param player 玩家
     * @param slotId 槽位ID
     * @param item 要检查的物品
     * @return 如果指定槽位中存在该物品则返回true
     */
    public static boolean hasItemInSlot(Player player, ResourceLocation slotId, Item item) {
        if (player == null || slotId == null || item == null) return false;

        Map<ResourceLocation, ItemStack> driverItems = getDriverItems(player);
        ItemStack stackInSlot = driverItems.get(slotId);

        if (stackInSlot != null && !stackInSlot.isEmpty() && stackInSlot.is(item)) {
            if (Config.DEVELOPER_MODE.get()) {
                RideBattleLib.LOGGER.debug("在槽位 {} 中找到物品: {}",
                        slotId, BuiltInRegistries.ITEM.getKey(item));
            }
            return true;
        }

        if (Config.DEVELOPER_MODE.get()) {
            RideBattleLib.LOGGER.debug("槽位 {} 中未找到物品: {}",
                    slotId, BuiltInRegistries.ITEM.getKey(item));
        }
        return false;
    }

    /**
     * 检查玩家驱动器的指定槽位中是否有指定物品（考虑NBT匹配）
     * @param player 玩家
     * @param slotId 槽位ID
     * @param itemStack 要检查的物品堆栈（包含NBT）
     * @return 如果指定槽位中存在匹配的物品则返回true
     */
    public static boolean hasItemInSlot(Player player, ResourceLocation slotId, ItemStack itemStack) {
        if (player == null || slotId == null || itemStack.isEmpty()) return false;

        Map<ResourceLocation, ItemStack> driverItems = getDriverItems(player);
        ItemStack stackInSlot = driverItems.get(slotId);

        if (stackInSlot != null && ItemStack.isSameItemSameComponents(stackInSlot, itemStack)) {
            if (Config.DEVELOPER_MODE.get()) {
                RideBattleLib.LOGGER.debug("在槽位 {} 中找到匹配物品: {} (NBT匹配)",
                        slotId, BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
            }
            return true;
        }

        return false;
    }

    /**
     * 获取指定槽位中的物品堆栈
     * @param player 玩家
     * @param slotId 槽位ID
     * @return 槽位中的物品堆栈，如果为空则返回ItemStack.EMPTY
     */
    public static ItemStack getItemForSlot(Player player, ResourceLocation slotId) {
        if (player == null || slotId == null) return ItemStack.EMPTY;

        Map<ResourceLocation, ItemStack> driverItems = getDriverItems(player);
        ItemStack stack = driverItems.get(slotId);

        return stack != null ? stack : ItemStack.EMPTY;
    }

    /**
     * 检查驱动器是否为空
     * @param player 玩家
     * @return 如果驱动器没有任何物品则返回true
     */
    public static boolean isDriverEmpty(Player player) {
        if (player == null) return true;

        Map<ResourceLocation, ItemStack> driverItems = getDriverItems(player);

        if (driverItems.isEmpty()) return true;

        for (ItemStack stack : driverItems.values()) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
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
            if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("播放音效{}", sound);
            player.level().playSound(null, player, sound, category, volume, pitch);
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
        if (Config.DEVELOPER_MODE.get()) RideBattleLib.LOGGER.debug("等待{}刻后执行{}", ticks, callback);
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
     * 快捷完成变身方法
     * @param ticks 等待游戏刻数
     * @param player 要完成变身的玩家
     */
    public static void completeIn(int ticks, Player player){
        scheduleTicks(ticks, () -> completeHenshin(player));
    }

    /**
     * 重置玩家所有变身相关状态（谨慎使用）
     */
    public static void resetPlayerState(Player player) {
        if (isTransformed(player)) {
            if (unTransform(player)) {
                RiderData data = player.getData(RiderAttachments.RIDER_DATA);
                data.setHenshinState(HenshinState.IDLE);
                data.setPendingFormId(null);
                data.setPenaltyCooldownEnd(0);
                data.setCurrentSkillIndex(0);

                player.removeTag("penalty_cooldown");
                player.removeTag("just_respawned");
            } else if (Config.DEVELOPER_MODE.get()) {
                RideBattleLib.LOGGER.debug("重置玩家变身状态失败");
            }
        }
    }
}
