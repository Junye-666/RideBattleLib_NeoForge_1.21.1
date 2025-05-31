package com.jpigeon.ridebattlelib.core.system.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PlayerPersistentData implements INBTSerializable<CompoundTag> {
    private Map<ResourceLocation, ItemStack> beltItems;
    private @Nullable TransformedAttachmentData transformedData;

    public PlayerPersistentData(Map<ResourceLocation, ItemStack> beltItems,
                                @Nullable TransformedAttachmentData transformedData) {
        this.beltItems = new HashMap<>(beltItems);
        this.transformedData = transformedData;
    }

    // Getter 方法
    public Map<ResourceLocation, ItemStack> beltItems() {
        return new HashMap<>(beltItems);
    }

    public @Nullable TransformedAttachmentData transformedData() {
        return transformedData;
    }

    // Setter 方法
    public void setBeltItems(Map<ResourceLocation, ItemStack> items) {
        this.beltItems = new HashMap<>(items);
    }

    public void setTransformedData(@Nullable TransformedAttachmentData data) {
        this.transformedData = data;
    }

    public static final Codec<PlayerPersistentData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC)
                            .fieldOf("beltItems")
                            .forGetter(PlayerPersistentData::beltItems),
                    TransformedAttachmentData.CODEC.optionalFieldOf("transformedData")
                            .forGetter(data -> Optional.ofNullable(data.transformedData))
            ).apply(instance, (beltItems, transformedDataOpt) ->
                    new PlayerPersistentData(beltItems, transformedDataOpt.orElse(null))
            )
    );

    // 序列化
    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();

        // 过滤空物品堆栈
        Map<ResourceLocation, ItemStack> validBeltItems = new HashMap<>();
        beltItems.forEach((key, stack) -> {
            if (!stack.isEmpty() && stack.getCount() > 0) {
                validBeltItems.put(key, stack);
            }
        });

        CompoundTag beltItemsTag = new CompoundTag();
        validBeltItems.forEach((key, stack) -> beltItemsTag.put(key.toString(), stack.save(provider)));
        tag.put("BeltItems", beltItemsTag);

        if (transformedData != null) {
            CompoundTag dataTag = new CompoundTag();
            dataTag.putString("riderId", transformedData.riderId().toString());
            dataTag.putString("formId", transformedData.formId().toString());

            // 序列化 originalGear
            CompoundTag gearTag = new CompoundTag();
            transformedData.originalGear().forEach((slot, stack) -> gearTag.put(slot.getName(), stack.isEmpty() ? new CompoundTag() : stack.save(provider)));
            dataTag.put("originalGear", gearTag);

            // 新增 beltSnapshot 序列化
            CompoundTag snapshotTag = new CompoundTag();
            transformedData.beltSnapshot().forEach((id, stack) -> snapshotTag.put(id.toString(), stack.isEmpty() ? new CompoundTag() : stack.save(provider)));
            dataTag.put("beltSnapshot", snapshotTag);

            tag.put("TransformedData", dataTag);
        }

        return tag;
    }

    // 反序列化（现在可以直接修改内部状态）
    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag tag) {
        beltItems.clear();

        if (tag.contains("BeltItems", CompoundTag.TAG_COMPOUND)) {
            CompoundTag beltItemsTag = tag.getCompound("BeltItems");
            for (String key : beltItemsTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) {
                    ItemStack stack = ItemStack.parse(provider, beltItemsTag.getCompound(key))
                            .orElse(ItemStack.EMPTY);
                    if (!stack.isEmpty()) {
                        beltItems.put(id, stack);
                    }
                }
            }
        }

        if (tag.contains("TransformedData", CompoundTag.TAG_COMPOUND)) {
            CompoundTag dataTag = tag.getCompound("TransformedData");
            transformedData = new TransformedAttachmentData(
                    ResourceLocation.tryParse(dataTag.getString("riderId")),
                    ResourceLocation.tryParse(dataTag.getString("formId")),
                    loadOriginalGear(provider, dataTag.getCompound("originalGear")),
                    // 新增 beltSnapshot 的反序列化
                    loadBeltSnapshot(provider, dataTag.getCompound("beltSnapshot"))
            );
        } else {
            transformedData = null;
        }
    }

    private Map<EquipmentSlot, ItemStack> loadOriginalGear(HolderLookup.Provider provider, CompoundTag gearTag) {
        Map<EquipmentSlot, ItemStack> gear = new HashMap<>();
        for (String key : gearTag.getAllKeys()) {
            EquipmentSlot slot = EquipmentSlot.byName(key);
            if (slot != null) {
                if (gearTag.getString(key).equals("EMPTY")) {
                    // 特殊标记的空物品
                    gear.put(slot, ItemStack.EMPTY);
                } else {
                    ItemStack stack = ItemStack.parse(provider, gearTag.getCompound(key))
                            .orElse(ItemStack.EMPTY);
                    gear.put(slot, stack);
                }
            }
        }
        return gear;
    }

    private Map<ResourceLocation, ItemStack> loadBeltSnapshot(HolderLookup.Provider provider, CompoundTag beltTag) {
        Map<ResourceLocation, ItemStack> snapshot = new HashMap<>();
        for (String key : beltTag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                ItemStack stack = ItemStack.parse(provider, beltTag.getCompound(key))
                        .orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    snapshot.put(id, stack);
                }
            }
        }
        return snapshot;
    }

    public String toString() {
        return "PlayerPersistentData{" +
                "beltItems=" + beltItems +
                ", transformedData=" + transformedData +
                '}';
    }
}



