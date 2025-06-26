package com.jpigeon.ridebattlelib.core.system.attachment;

import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PlayerPersistentData {
    public Map<ResourceLocation, Map<ResourceLocation, ItemStack>> riderBeltItems;
    private @Nullable TransformedAttachmentData transformedData;
    private HenshinState henshinState;
    private @Nullable ResourceLocation pendingFormId;
    private long penaltyCooldownEnd;

    public PlayerPersistentData(
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> riderBeltItems,
            @Nullable TransformedAttachmentData transformedData,
            HenshinState henshinState,
            @Nullable ResourceLocation pendingFormId,
            long penaltyCooldownEnd
    ) {
        this.riderBeltItems = riderBeltItems != null ?
                new HashMap<>(riderBeltItems) : new HashMap<>();
        this.transformedData = transformedData;
        this.henshinState = henshinState;
        this.pendingFormId = pendingFormId;
        this.penaltyCooldownEnd = penaltyCooldownEnd;
    }

    //====================Setter方法====================

    public void setHenshinState(HenshinState state) {
        this.henshinState = state;
    }


    public void setPendingFormId(@Nullable ResourceLocation formId) {
        this.pendingFormId = formId;
    }

    // Setter 方法
    public void setBeltItems(ResourceLocation riderId, Map<ResourceLocation, ItemStack> items) {
        if (items == null) {
            riderBeltItems.remove(riderId);
        } else {
            riderBeltItems.put(riderId, new HashMap<>(items));
        }
    }

    public void setPenaltyCooldownEnd(long endTime) {
        this.penaltyCooldownEnd = endTime;
    }

    //====================Getter方法====================

    public Map<ResourceLocation, ItemStack> getBeltItems(ResourceLocation riderId) {
        return riderBeltItems.getOrDefault(riderId, new HashMap<>());
    }

    public @Nullable TransformedAttachmentData transformedData() {
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

    //====================Codec====================

    public static final Codec<PlayerPersistentData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(

                    Codec.unboundedMap(
                                    ResourceLocation.CODEC,
                                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC)
                            ).optionalFieldOf("riderBeltItems", new HashMap<>())
                            .forGetter(data -> data.riderBeltItems),

                    TransformedAttachmentData.CODEC.optionalFieldOf("transformedData")
                            .forGetter(data -> Optional.ofNullable(data.transformedData)),


                    HenshinState.CODEC.fieldOf("henshinState")
                            .forGetter(data -> data.henshinState),


                    ResourceLocation.CODEC.optionalFieldOf("pendingFormId")
                            .forGetter(data -> Optional.ofNullable(data.pendingFormId))
            ).apply(instance, (beltItems, transformedDataOpt, henshinState, pendingFormIdOpt) ->
                    new PlayerPersistentData(
                            beltItems != null ? beltItems : new HashMap<>(),
                            transformedDataOpt.orElse(null),
                            henshinState,
                            pendingFormIdOpt.orElse(null),
                            System.currentTimeMillis()
                    )
            )
    );
}
