package com.jpigeon.ridebattlelib.core.system.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public record PlayerPersistentData(
        Map<ResourceLocation, ItemStack> beltItems,
        @Nullable TransformedAttachmentData transformedData
) {
    public static final Codec<PlayerPersistentData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC).fieldOf("beltItems").forGetter(PlayerPersistentData::beltItems),
                    TransformedAttachmentData.CODEC.optionalFieldOf("transformedData").forGetter(data -> Optional.ofNullable(data.transformedData))
            ).apply(instance, (beltItems, transformedData) -> new PlayerPersistentData(beltItems, transformedData.orElse(null)))
    );
}

