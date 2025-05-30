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
                    // 使用自定义编解码器处理原始装备
                    originalGearCodec().fieldOf("originalGear").forGetter(TransformedAttachmentData::originalGear)
            ).apply(instance, TransformedAttachmentData::new)
    );

    // 自定义原始装备编解码器
    private static Codec<Map<EquipmentSlot, ItemStack>> originalGearCodec() {
        return Codec.unboundedMap(
                EquipmentSlot.CODEC,
                ItemStack.OPTIONAL_CODEC // 使用OPTIONAL_CODEC处理空物品
        );
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("riderId", riderId.toString());
        tag.putString("formId", formId.toString());

        CompoundTag gearTag = new CompoundTag();
        originalGear.forEach((slot, stack) -> {
            // 使用特殊标记保存空物品
            if (stack.isEmpty()) {
                gearTag.putString(slot.getName(), "EMPTY");
            } else {
                gearTag.put(slot.getName(), stack.save(provider));
            }
        });
        tag.put("originalGear", gearTag);

        return tag;
    }
}