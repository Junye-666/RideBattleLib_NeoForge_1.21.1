package com.jpigeon.ridebattlelib.core.system.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

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

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("riderId", riderId.toString());
        tag.putString("formId", formId.toString());

        CompoundTag gearTag = new CompoundTag();
        originalGear.forEach((slot, stack) -> gearTag.put(slot.getName(), stack.save(provider)));
        tag.put("originalGear", gearTag);

        return tag;
    }
}
