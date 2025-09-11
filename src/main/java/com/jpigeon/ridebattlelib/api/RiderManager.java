package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.driver.DriverSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SwitchFormPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.UnhenshinPacket;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import com.jpigeon.ridebattlelib.core.system.skill.SkillSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 假面骑士系统快捷方法管理器。
 * 提供静态方法供其他模组调用，如变身、形态切换、物品管理等。
 * 所有方法均线程安全，可在客户端或服务端调用。
 */
public final class RiderManager {
    private RiderManager() {} // 防止实例化

    // ================ 变身系统快捷方法 ================
    /**
     * 尝试让玩家变身。
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
     * @param player 玩家
     * @return 是否成功切换
     */
    public static boolean switchForm(Player player, ResourceLocation newFormId) {
        if (!HenshinSystem.INSTANCE.isTransformed(player)){
            PacketHandler.sendToServer(new SwitchFormPacket(player.getUUID(), newFormId));
            return true;
        }
        return false;
    }

    /**
     * 快捷检查变身状态
     */
    public static boolean isTransformed(Player player) {
        return HenshinSystem.INSTANCE.isTransformed(player);
    }

    /**
     * 快捷完成变身序列
     */
    public static void completeHenshin(Player player){
        DriverActionManager.INSTANCE.completeTransformation(player);
    }

    // ================驱动器系统快捷方法 ================
    // 获取玩家驱动器物品
    public static Map<ResourceLocation, ItemStack> getDriverItems(Player player) {
        return DriverSystem.INSTANCE.getDriverItems(player);
    }

    // 插入物品至驱动器
    public static boolean insertDriverItem(Player player, ResourceLocation slotId, ItemStack stack) {
        return DriverSystem.INSTANCE.insertItem(player, slotId, stack);
    }

    // 将单个物品从驱动器中取出
    public static ItemStack extractSingleItem(Player player, ResourceLocation slotId) {
        return DriverSystem.INSTANCE.extractItem(player, slotId);
    }

    // 返还所有驱动器物品
    public static void returnDriverItems(Player player) {
        DriverSystem.INSTANCE.returnItems(player);
    }

    // ================ 吃瘪系统快捷方法 ================
    // 强制取消变身
    public static void forceUntransform(Player player) {
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            PenaltySystem.PENALTY_SYSTEM.forceUnhenshin(player);
        }
    }

    // 开始冷却
    public static void applyCooldown(Player player, int seconds) {
        PenaltySystem.PENALTY_SYSTEM.startCooldown(player, seconds);
    }

    // 检查是否在冷却中
    public static boolean isInCooldown(Player player) {
        return PenaltySystem.PENALTY_SYSTEM.isInCooldown(player);
    }

    // ================ 技能系统快捷方法 ================
    // 触发技能
    public static boolean triggerSkill(Player player, ResourceLocation formId, ResourceLocation skillId){
        return SkillSystem.triggerSkillEvent(player, formId, skillId);
    }

    // ================ 快速获取 ================
    // 获取当前骑士配置
    @Nullable
    public static RiderConfig getActiveRiderConfig(Player player) {
        return RiderConfig.findActiveDriverConfig(player);
    }

    // 获取当前形态ID
    @Nullable
    public static ResourceLocation getCurrentForm(Player player) {
        HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
        return data != null ? data.formId() : null;
    }

    // 检查是否特定骑士
    public static boolean isSpecificRider(Player player, ResourceLocation riderId) {
        RiderConfig config = getActiveRiderConfig(player);
        return config != null && config.getRiderId().equals(riderId);
    }

    // 强制刷新客户端状态
    public static void syncClientState(ServerPlayer player) {
        HenshinSystem.syncTransformedState(player);
        DriverSystem.INSTANCE.syncDriverData(player);
    }
}
