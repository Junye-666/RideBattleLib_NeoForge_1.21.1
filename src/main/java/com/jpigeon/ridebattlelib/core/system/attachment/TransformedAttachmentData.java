package com.jpigeon.ridebattlelib.core.system.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

// 变身状态数据（简化版）
public record TransformedAttachmentData(
        ResourceLocation riderId,
        ResourceLocation formId,
        Map<EquipmentSlot, ItemStack> originalGear
) {
    public static final Codec<TransformedAttachmentData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("riderId").forGetter(TransformedAttachmentData::riderId),
                    ResourceLocation.CODEC.fieldOf("formId").forGetter(TransformedAttachmentData::formId),
                    Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).fieldOf("originalGear").forGetter(TransformedAttachmentData::originalGear)
            ).apply(instance, TransformedAttachmentData::new)
    );
}
