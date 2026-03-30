package com.jpigeon.ridebattlelib.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public record HenshinSessionData(
        ResourceLocation riderId,
        ResourceLocation formId,
        Map<EquipmentSlot, ItemStack> originalGear,
        Map<ResourceLocation, ItemStack> driverSnapshot
) {
    public static final Codec<HenshinSessionData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("riderId").forGetter(HenshinSessionData::riderId),
                    ResourceLocation.CODEC.fieldOf("formId").forGetter(HenshinSessionData::formId),
                    Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.OPTIONAL_CODEC)
                            .xmap(map -> {
                                Map<EquipmentSlot, ItemStack> filtered = new EnumMap<>(EquipmentSlot.class);
                                map.forEach((k, v) -> {
                                    if (!v.isEmpty()) {
                                        filtered.put(k, v);
                                    }
                                });
                                return filtered;
                            }, map -> map)
                            .fieldOf("originalGear")
                            .forGetter(HenshinSessionData::originalGear),
                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.OPTIONAL_CODEC)
                            .xmap(map -> {
                                Map<ResourceLocation, ItemStack> filtered = new HashMap<>();
                                map.forEach((k, v) -> {
                                    if (!v.isEmpty()) {
                                        filtered.put(k, v);
                                    }
                                });
                                return filtered;
                            }, map -> map)
                            .fieldOf("driverSnapshot")
                            .forGetter(HenshinSessionData::driverSnapshot)
            ).apply(instance, HenshinSessionData::new)
    );
}