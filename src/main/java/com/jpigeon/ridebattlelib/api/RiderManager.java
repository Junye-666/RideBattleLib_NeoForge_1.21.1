package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.event.FormSwitchEvent;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltySystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Consumer;

public final class RiderManager {
    private RiderManager() {} // 防止实例化

    // ================ 变身系统快捷方法 ================
    public static boolean transform(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        return config != null && HenshinSystem.INSTANCE.henshin(player, config.getRiderId());
    }

    public static void forceUntransform(Player player) {
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            PenaltySystem.PENALTY_SYSTEM.forceUnhenshin(player);
        }
    }

    public static boolean isTransformed(Player player) {
        return HenshinSystem.INSTANCE.isTransformed(player);
    }

    public static void switchForm(Player player, ResourceLocation newFormId) {
        HenshinSystem.INSTANCE.switchForm(player, newFormId);
    }

    // ================ 腰带系统快捷方法 ================
    public static Map<ResourceLocation, ItemStack> getBeltItems(Player player) {
        return BeltSystem.INSTANCE.getBeltItems(player);
    }

    public static boolean insertBeltItem(Player player, ResourceLocation slotId, ItemStack stack) {
        return BeltSystem.INSTANCE.insertItem(player, slotId, stack);
    }

    public static ItemStack extractBeltItem(Player player, ResourceLocation slotId) {
        return BeltSystem.INSTANCE.extractItem(player, slotId);
    }

    public static void returnBeltItems(Player player) {
        BeltSystem.INSTANCE.returnItems(player);
    }

    // ================ 惩罚系统快捷方法 ================
    public static void applyCooldown(Player player, int seconds) {
        PenaltySystem.PENALTY_SYSTEM.startCooldown(player, seconds);
    }

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

    // ================ 事件监听快捷注册 ================
    public static void registerHenshinListener(Consumer<HenshinEvent> handler) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, HenshinEvent.class, event -> {
            if (event instanceof HenshinEvent.Post) handler.accept(event);
        });
    }

    public static void registerFormSwitchListener(Consumer<FormSwitchEvent> handler) {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, FormSwitchEvent.class, event -> {
            if (event instanceof FormSwitchEvent.Post) handler.accept(event);
        });
    }
}
