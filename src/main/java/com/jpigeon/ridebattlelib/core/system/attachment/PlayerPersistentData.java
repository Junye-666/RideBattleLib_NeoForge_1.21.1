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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PlayerPersistentData implements INBTSerializable<CompoundTag> {
    public Map<ResourceLocation, Map<ResourceLocation, ItemStack>> riderBeltItems;
    private @Nullable TransformedAttachmentData transformedData;

    public PlayerPersistentData(
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> riderBeltItems,
            @Nullable TransformedAttachmentData transformedData
    ) {
        // 确保不会出现空指针
        this.riderBeltItems = riderBeltItems != null ?
                new HashMap<>(riderBeltItems) :
                new HashMap<>();
        this.transformedData = transformedData;
    }

    // Getter 方法
    public Map<ResourceLocation, ItemStack> getBeltItems(ResourceLocation riderId) {
        return riderBeltItems.getOrDefault(riderId, new HashMap<>());
    }

    public @Nullable TransformedAttachmentData transformedData() {
        return transformedData;
    }

    // Setter 方法
    public void setBeltItems(ResourceLocation riderId, Map<ResourceLocation, ItemStack> items) {
        if (items == null) {
            riderBeltItems.remove(riderId);
        } else {
            riderBeltItems.put(riderId, new HashMap<>(items));
        }
    }

    public void setTransformedData(@Nullable TransformedAttachmentData data) {
        this.transformedData = data;
    }

    public static final Codec<PlayerPersistentData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    // 修复 riderBeltItems 的编解码器
                    Codec.unboundedMap(
                                    ResourceLocation.CODEC,
                                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC)
                            ).fieldOf("riderBeltItems")
                            .forGetter(data -> data.riderBeltItems),

                    TransformedAttachmentData.CODEC.optionalFieldOf("transformedData")
                            .forGetter(data -> Optional.ofNullable(data.transformedData))
            ).apply(instance, (beltItems, transformedDataOpt) ->
                    new PlayerPersistentData(
                            beltItems != null ? beltItems : new HashMap<>(),
                            transformedDataOpt.orElse(null)
                    )
            )
    );

    // 序列化
    @Override
    public CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag tag = new CompoundTag();

        // 序列化 riderBeltItems
        CompoundTag riderBeltItemsTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, ItemStack>> entry : riderBeltItems.entrySet()) {
            ResourceLocation riderId = entry.getKey();
            CompoundTag beltItemsTag = new CompoundTag();

            for (Map.Entry<ResourceLocation, ItemStack> slotEntry : entry.getValue().entrySet()) {
                ResourceLocation slotId = slotEntry.getKey();
                ItemStack stack = slotEntry.getValue();

                if (!stack.isEmpty()) {
                    beltItemsTag.put(
                            slotId.toString(),
                            stack.save(provider)
                    );
                }
            }

            riderBeltItemsTag.put(riderId.toString(), beltItemsTag);
        }
        tag.put("RiderBeltItems", riderBeltItemsTag);

        // 序列化 transformedData
        if (transformedData != null) {
            CompoundTag dataTag = new CompoundTag();
            dataTag.putString("riderId", transformedData.riderId().toString());
            dataTag.putString("formId", transformedData.formId().toString());

            // 序列化 originalGear
            CompoundTag gearTag = new CompoundTag();
            transformedData.originalGear().forEach((slot, stack) -> {
                gearTag.put(slot.getName(), stack.save(provider));
            });
            dataTag.put("originalGear", gearTag);

            // 序列化 beltSnapshot
            CompoundTag snapshotTag = new CompoundTag();
            transformedData.beltSnapshot().forEach((id, stack) -> {
                snapshotTag.put(id.toString(), stack.save(provider));
            });
            dataTag.put("beltSnapshot", snapshotTag);

            tag.put("TransformedData", dataTag);
        }

        return tag;
    }

    // 反序列化
    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, CompoundTag tag) {
        riderBeltItems.clear();

        // 反序列化 riderBeltItems
        if (tag.contains("RiderBeltItems", CompoundTag.TAG_COMPOUND)) {
            CompoundTag riderBeltItemsTag = tag.getCompound("RiderBeltItems");
            for (String riderKey : riderBeltItemsTag.getAllKeys()) {
                ResourceLocation riderId = ResourceLocation.tryParse(riderKey);
                if (riderId == null) continue;

                CompoundTag beltItemsTag = riderBeltItemsTag.getCompound(riderKey);
                Map<ResourceLocation, ItemStack> beltItems = new HashMap<>();

                for (String slotKey : beltItemsTag.getAllKeys()) {
                    ResourceLocation slotId = ResourceLocation.tryParse(slotKey);
                    if (slotId == null) continue;

                    ItemStack stack = ItemStack.parse(provider, beltItemsTag.getCompound(slotKey))
                            .orElse(ItemStack.EMPTY);

                    if (!stack.isEmpty()) {
                        beltItems.put(slotId, stack);
                    }
                }

                riderBeltItems.put(riderId, beltItems);
            }
        }

        // 反序列化 transformedData
        if (tag.contains("TransformedData", CompoundTag.TAG_COMPOUND)) {
            CompoundTag dataTag = tag.getCompound("TransformedData");
            ResourceLocation riderId = ResourceLocation.tryParse(dataTag.getString("riderId"));
            ResourceLocation formId = ResourceLocation.tryParse(dataTag.getString("formId"));

            // 加载 originalGear
            Map<EquipmentSlot, ItemStack> originalGear = loadOriginalGear(
                    provider,
                    dataTag.getCompound("originalGear")
            );

            // 加载 beltSnapshot
            Map<ResourceLocation, ItemStack> beltSnapshot = loadBeltSnapshot(
                    provider,
                    dataTag.getCompound("beltSnapshot")
            );

            transformedData = new TransformedAttachmentData(
                    riderId,
                    formId,
                    originalGear,
                    beltSnapshot
            );
        } else {
            transformedData = null;
        }
    }

    private Map<EquipmentSlot, ItemStack> loadOriginalGear(HolderLookup.Provider provider, CompoundTag gearTag) {
        Map<EquipmentSlot, ItemStack> gear = new EnumMap<>(EquipmentSlot.class);

        for (String key : gearTag.getAllKeys()) {
            EquipmentSlot slot = EquipmentSlot.byName(key);
            if (slot != null) {
                CompoundTag stackTag = gearTag.getCompound(key);
                ItemStack stack = ItemStack.parse(provider, stackTag).orElse(ItemStack.EMPTY);
                gear.put(slot, stack);
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
                "beltItems=" + riderBeltItems +
                ", transformedData=" + transformedData +
                '}';
    }
}



