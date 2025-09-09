package com.jpigeon.ridebattlelib.core.system.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public record TransformedAttachmentData(
        ResourceLocation riderId,
        ResourceLocation formId,
        Map<EquipmentSlot, ItemStack> originalGear,
        Map<ResourceLocation, ItemStack> driverSnapshot
) {
    public static final Codec<TransformedAttachmentData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("riderId").forGetter(TransformedAttachmentData::riderId),
                    ResourceLocation.CODEC.fieldOf("formId").forGetter(TransformedAttachmentData::formId),

                    Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.OPTIONAL_CODEC)
                            .fieldOf("originalGear").forGetter(TransformedAttachmentData::originalGear),

                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.OPTIONAL_CODEC)
                            .fieldOf("driverSnapshot").forGetter(TransformedAttachmentData::driverSnapshot)
            ).apply(instance, TransformedAttachmentData::new)
    );
}
