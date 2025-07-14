package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RiderData {
    public Map<ResourceLocation, Map<ResourceLocation, ItemStack>> mainBeltItems;
    public Map<ResourceLocation, Map<ResourceLocation, ItemStack>> auxBeltItems;
    private final @Nullable TransformedAttachmentData transformedData;
    private HenshinState henshinState;
    private @Nullable ResourceLocation pendingFormId;
    private long penaltyCooldownEnd;
    private int currentSkillIndex = 0; // 当前选中的技能索引

    public RiderData(
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> mainBeltItems,
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> auxBeltItems,
            @Nullable TransformedAttachmentData transformedData,
            HenshinState henshinState,
            @Nullable ResourceLocation pendingFormId,
            long penaltyCooldownEnd,
            int currentSkillIndex
    ) {
        this.mainBeltItems = mainBeltItems != null ?
                new HashMap<>(mainBeltItems) : new HashMap<>();
        this.auxBeltItems = auxBeltItems != null ?
                new HashMap<>(auxBeltItems) : new HashMap<>();
        this.transformedData = transformedData;
        this.henshinState = henshinState;
        this.pendingFormId = pendingFormId;
        this.penaltyCooldownEnd = penaltyCooldownEnd;
        this.currentSkillIndex = currentSkillIndex;
    }

    //====================Setter方法====================

    public void setAuxBeltItems(ResourceLocation riderId, Map<ResourceLocation, ItemStack> items) {
        if (items == null) {
            auxBeltItems.remove(riderId);
        } else {
            auxBeltItems.put(riderId, new HashMap<>(items));
        }
    }

    public void setHenshinState(HenshinState state) {
        this.henshinState = state;
    }

    public void setPendingFormId(@Nullable ResourceLocation formId) {
        this.pendingFormId = formId;
    }

    // Setter 方法
    public void setBeltItems(ResourceLocation riderId, Map<ResourceLocation, ItemStack> items) {
        if (items == null) {
            mainBeltItems.remove(riderId);
        } else {
            mainBeltItems.put(riderId, new HashMap<>(items));
        }
    }

    public void setPenaltyCooldownEnd(long endTime) {
        this.penaltyCooldownEnd = endTime;
    }

    public void setCurrentSkillIndex(int index) {
        this.currentSkillIndex = index;
    }

    //====================Getter方法====================

    public Map<ResourceLocation, ItemStack> getBeltItems(ResourceLocation riderId) {
        return mainBeltItems.getOrDefault(riderId, new HashMap<>());
    }

    public ItemStack getAuxBeltItems(ResourceLocation riderId, ResourceLocation slotId) {
        return auxBeltItems.getOrDefault(riderId, Collections.emptyMap())
                .getOrDefault(slotId, ItemStack.EMPTY);
    }

    public @Nullable TransformedAttachmentData getTransformedData() {
        return transformedData;
    }

    public HenshinState getHenshinState() {
        return henshinState;
    }

    @Nullable
    public ResourceLocation getPendingFormId() {
        return pendingFormId;
    }

    public long getPenaltyCooldownEnd() {
        return penaltyCooldownEnd;
    }

    public boolean isInPenaltyCooldown() {
        return System.currentTimeMillis() < penaltyCooldownEnd;
    }

    public int getCurrentSkillIndex() {
        return currentSkillIndex;
    }

    //====================Codec====================

    public static final Codec<RiderData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(

                    Codec.unboundedMap(
                                    ResourceLocation.CODEC,
                                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.OPTIONAL_CODEC)
                            ).optionalFieldOf("mainBeltItems", new HashMap<>())
                            .forGetter(data -> data.mainBeltItems),

                    Codec.unboundedMap(ResourceLocation.CODEC,
                                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.OPTIONAL_CODEC)
                            ).optionalFieldOf("auxBeltItems", new HashMap<>())
                            .forGetter(data -> data.auxBeltItems),

                    TransformedAttachmentData.CODEC.optionalFieldOf("getTransformedData")
                            .forGetter(data -> Optional.ofNullable(data.transformedData)),


                    HenshinState.CODEC.fieldOf("henshinState")
                            .forGetter(data -> data.henshinState),


                    ResourceLocation.CODEC.optionalFieldOf("pendingFormId")
                            .forGetter(data -> Optional.ofNullable(data.pendingFormId)),

                    Codec.LONG.fieldOf("penaltyCooldownEnd")
                            .forGetter(data -> data.penaltyCooldownEnd),

                    Codec.INT.optionalFieldOf("currentSkillIndex", 0)
                            .forGetter(RiderData::getCurrentSkillIndex)
            ).apply(instance, (riderBeltItems, auxBeltItems, transformedDataOpt, henshinState, pendingFormIdOpt, penaltyCooldownEnd, currentSkillIndex) ->
                    new RiderData(
                            riderBeltItems != null ? riderBeltItems : new HashMap<>(),
                            auxBeltItems,
                            transformedDataOpt.orElse(null),
                            henshinState,
                            pendingFormIdOpt.orElse(null),
                            penaltyCooldownEnd,
                            currentSkillIndex
                    )
            )
    );
}
