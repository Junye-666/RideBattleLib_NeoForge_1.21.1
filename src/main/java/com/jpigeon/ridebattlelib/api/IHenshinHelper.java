package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 变身助手接口 - 简化版本
 */
public interface IHenshinHelper {
    /**
     * 执行变身，包含设置变身数据
     */
    void performHenshin(Player player, RiderConfig config, ResourceLocation formId);

    /**
     * 执行形态切换
     */
    void performFormSwitch(Player player, ResourceLocation newFormId);

    /**
     * 恢复变身状态 - 使用统一的数据结构
     */
    void restoreTransformedState(Player player, HenshinSystem.TransformedData data);

    /**
     * 移除变身状态
     */
    void removeTransformed(Player player);

    /**
     * 保存变身快照（内部使用）
     */
    void saveTransformedSnapshot(Player player, RiderConfig config, ResourceLocation formId,
                                 Map<EquipmentSlot, ItemStack> originalGear,
                                 Map<ResourceLocation, ItemStack> driverSnapshot);
}