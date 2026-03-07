package com.jpigeon.ridebattlelib.core.system.henshin.helper.data;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public record TransformedData(
        RiderConfig config,
        ResourceLocation formId,
        Map<EquipmentSlot, ItemStack> originalGear,
        Map<ResourceLocation, ItemStack> driverSnapshot
) {
}
