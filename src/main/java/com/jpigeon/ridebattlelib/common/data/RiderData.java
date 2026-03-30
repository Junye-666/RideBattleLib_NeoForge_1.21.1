package com.jpigeon.ridebattlelib.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RiderData {
    // ========== 持久数据 ==========
    private final Map<ResourceLocation, Map<ResourceLocation, ItemStack>> mainDriverItems;
    private final Map<ResourceLocation, Map<ResourceLocation, ItemStack>> auxDriverItems;
    private long penaltyCooldownEnd;
    private int currentSkillIndex;

    // ========== 临时数据 ==========
    @Nullable
    private HenshinSessionData sessionData;
    private HenshinState state;
    @Nullable
    private ResourceLocation pendingFormId;

    // ========== 构造器 ==========
    public RiderData() {
        this.mainDriverItems = new HashMap<>();
        this.auxDriverItems = new HashMap<>();
        this.penaltyCooldownEnd = 0;
        this.currentSkillIndex = 0;
        this.sessionData = null;
        this.state = HenshinState.IDLE;
        this.pendingFormId = null;
    }

    // 深拷贝构造
    public RiderData(RiderData other) {
        this.mainDriverItems = deepCopyMap(other.mainDriverItems);
        this.auxDriverItems = deepCopyMap(other.auxDriverItems);
        this.penaltyCooldownEnd = other.penaltyCooldownEnd;
        this.currentSkillIndex = other.currentSkillIndex;
        this.sessionData = other.sessionData; // 记录是不可变的，直接引用
        this.state = other.state;
        this.pendingFormId = other.pendingFormId;
    }

    // ========== Getters ==========
    public Map<ResourceLocation, Map<ResourceLocation, ItemStack>> getMainDriverItems() {
        return mainDriverItems;
    }

    public Map<ResourceLocation, Map<ResourceLocation, ItemStack>> getAuxDriverItems() {
        return auxDriverItems;
    }

    public long getPenaltyCooldownEnd() {
        return penaltyCooldownEnd;
    }

    public int getCurrentSkillIndex() {
        return currentSkillIndex;
    }

    @Nullable
    public HenshinSessionData getSessionData() {
        return sessionData;
    }

    public HenshinState getState() {
        return state;
    }

    @Nullable
    public ResourceLocation getPendingFormId() {
        return pendingFormId;
    }

    // ========== Setters ==========
    public void setMainDriverItems(Map<ResourceLocation, Map<ResourceLocation, ItemStack>> items) {
        this.mainDriverItems.clear();
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, ItemStack>> entry : items.entrySet()) {
            Map<ResourceLocation, ItemStack> innerMap = new HashMap<>();
            for (Map.Entry<ResourceLocation, ItemStack> inner : entry.getValue().entrySet()) {
                if (!inner.getValue().isEmpty()) {
                    innerMap.put(inner.getKey(), inner.getValue());
                }
            }
            if (!innerMap.isEmpty()) {
                this.mainDriverItems.put(entry.getKey(), innerMap);
            }
        }
    }

    public void setAuxDriverItems(Map<ResourceLocation, Map<ResourceLocation, ItemStack>> items) {
        this.auxDriverItems.clear();
        for (Map.Entry<ResourceLocation, Map<ResourceLocation, ItemStack>> entry : items.entrySet()) {
            Map<ResourceLocation, ItemStack> innerMap = new HashMap<>();
            for (Map.Entry<ResourceLocation, ItemStack> inner : entry.getValue().entrySet()) {
                if (!inner.getValue().isEmpty()) {
                    innerMap.put(inner.getKey(), inner.getValue());
                }
            }
            if (!innerMap.isEmpty()) {
                this.auxDriverItems.put(entry.getKey(), innerMap);
            }
        }
    }

    public void setPenaltyCooldownEnd(long endTime) {
        this.penaltyCooldownEnd = endTime;
    }

    public void setCurrentSkillIndex(int index) {
        this.currentSkillIndex = index;
    }

    public void startHenshinSession(HenshinSessionData session) {
        this.sessionData = session;
        this.state = HenshinState.TRANSFORMED;
        this.pendingFormId = null;
    }

    public void endHenshinSession() {
        this.sessionData = null;
        this.state = HenshinState.IDLE;
        this.pendingFormId = null;
    }

    public void setState(HenshinState state) {
        this.state = state;
    }

    public void setPendingFormId(@Nullable ResourceLocation id) {
        this.pendingFormId = id;
    }

    // ========== 辅助方法 ==========
    public boolean isInPenaltyCooldown() {
        return System.currentTimeMillis() < penaltyCooldownEnd;
    }

    public boolean isTransformed() {
        return sessionData != null;
    }

    // ========== 深拷贝工具 ==========
    private static Map<ResourceLocation, Map<ResourceLocation, ItemStack>> deepCopyMap(
            Map<ResourceLocation, Map<ResourceLocation, ItemStack>> original) {
        Map<ResourceLocation, Map<ResourceLocation, ItemStack>> copy = new HashMap<>();
        for (var entry : original.entrySet()) {
            Map<ResourceLocation, ItemStack> innerCopy = new HashMap<>();
            for (var innerEntry : entry.getValue().entrySet()) {
                innerCopy.put(innerEntry.getKey(), innerEntry.getValue().copy());
            }
            copy.put(entry.getKey(), innerCopy);
        }
        return copy;
    }

    // ========== Codec 定义 ==========
    public static final Codec<RiderData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    // 持久数据
                    Codec.unboundedMap(ResourceLocation.CODEC,
                                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC))
                            .optionalFieldOf("mainDriverItems", new HashMap<>())
                            .forGetter(data -> data.mainDriverItems),

                    Codec.unboundedMap(ResourceLocation.CODEC,
                                    Codec.unboundedMap(ResourceLocation.CODEC, ItemStack.CODEC))
                            .optionalFieldOf("auxDriverItems", new HashMap<>())
                            .forGetter(data -> data.auxDriverItems),

                    Codec.LONG.fieldOf("penaltyCooldownEnd")
                            .forGetter(data -> data.penaltyCooldownEnd),

                    Codec.INT.fieldOf("currentSkillIndex")
                            .forGetter(data -> data.currentSkillIndex),

                    // 临时数据
                    HenshinSessionData.CODEC.optionalFieldOf("sessionData")
                            .forGetter(data -> java.util.Optional.ofNullable(data.sessionData)),

                    HenshinState.CODEC.fieldOf("state")
                            .forGetter(data -> data.state),

                    ResourceLocation.CODEC.optionalFieldOf("pendingFormId")
                            .forGetter(data -> java.util.Optional.ofNullable(data.pendingFormId))

            ).apply(instance, (main, aux, cooldown, skillIndex, sessionOpt, state, pendingOpt) -> {
                RiderData data = new RiderData();
                data.setMainDriverItems(Objects.requireNonNullElseGet(main, HashMap::new));
                data.setAuxDriverItems(Objects.requireNonNullElseGet(aux, HashMap::new));
                data.setPenaltyCooldownEnd(cooldown);
                data.setCurrentSkillIndex(skillIndex);
                sessionOpt.ifPresent(data::startHenshinSession);
                data.setState(state);
                data.setPendingFormId(pendingOpt.orElse(null));
                return data;
            })
    );
}
