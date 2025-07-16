package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SwitchFormPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.UnhenshinPacket;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;


public final class RiderManager {
    private RiderManager() {} // 防止实例化

    // ================ 变身系统快捷方法 ================
    // 快捷变身
    public static boolean transform(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config != null) {
            PacketHandler.sendToServer(new HenshinPacket(config.getRiderId()));
            return true;
        }
        return false;
    }

    // 快捷解除
    public static boolean unTransform(Player player) {
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            PacketHandler.sendToServer(new UnhenshinPacket());
            return true;
        }
        return false;
    }

    // 快捷切换形态
    public static void switchForm(Player player, ResourceLocation newFormId) {
        PacketHandler.sendToServer(new SwitchFormPacket(newFormId));
    }

    // 快捷检查变身状态
    public static boolean isTransformed(Player player) {
        return HenshinSystem.INSTANCE.isTransformed(player);
    }

    // ================ 腰带系统快捷方法 ================
    // 获取玩家腰带物品
    public static Map<ResourceLocation, ItemStack> getBeltItems(Player player) {
        return BeltSystem.INSTANCE.getBeltItems(player);
    }

    // 插入物品至腰带
    public static boolean insertBeltItem(Player player, ResourceLocation slotId, ItemStack stack) {
        return BeltSystem.INSTANCE.insertItem(player, slotId, stack);
    }

    // 将物品从腰带中取出
    public static ItemStack extractBeltItem(Player player, ResourceLocation slotId) {
        return BeltSystem.INSTANCE.extractItem(player, slotId);
    }

    // 返还所有腰带物品
    public static void returnBeltItems(Player player) {
        BeltSystem.INSTANCE.returnItems(player);
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
        BeltSystem.INSTANCE.syncBeltData(player);
    }
}
