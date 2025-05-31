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
        Map<EquipmentSlot, ItemStack> originalGear,
        Map<ResourceLocation, ItemStack> beltSnapshot // 新增：变身时的腰带快照
) {
    public static final Codec<TransformedAttachmentData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("riderId").forGetter(TransformedAttachmentData::riderId),
                    ResourceLocation.CODEC.fieldOf("formId").forGetter(TransformedAttachmentData::formId),
                    originalGearCodec().fieldOf("originalGear").forGetter(TransformedAttachmentData::originalGear),
                    // 新增 beltSnapshot 的编解码器（与 originalGear 相同结构）
                    Codec.unboundedMap(
                            ResourceLocation.CODEC,
                            ItemStack.OPTIONAL_CODEC
                    ).fieldOf("beltSnapshot").forGetter(TransformedAttachmentData::beltSnapshot)
            ).apply(instance, TransformedAttachmentData::new) // 现在参数数量匹配
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

        // 序列化 originalGear
        CompoundTag gearTag = new CompoundTag();
        originalGear.forEach((slot, stack) -> {
            gearTag.put(slot.getName(), stack.isEmpty() ? new CompoundTag() : stack.save(provider));
        });
        tag.put("originalGear", gearTag);

        // 序列化 beltSnapshot
        CompoundTag beltTag = new CompoundTag();
        beltSnapshot.forEach((slotId, stack) -> {
            beltTag.put(slotId.toString(), stack.isEmpty() ? new CompoundTag() : stack.save(provider));
        });
        tag.put("beltSnapshot", beltTag);

        return tag;
    }
}