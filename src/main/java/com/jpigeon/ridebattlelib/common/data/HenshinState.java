package com.jpigeon.ridebattlelib.common.data;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum HenshinState implements StringRepresentable {
    /**
     * 未变身（无 session，无 pendingFormId）。
     */
    IDLE("idle"),
    /**
     * 变身/切形态进行中。
     * <p>
     * 统一代表原 TRANSFORMING（auto 等待）+ PAUSED（等待 completeHenshin）
     * + SWITCHING（形态切换中）。由 {@code pendingFormId} + {@code session}
     * 区分具体语义，state 层面不再细分。
     */
    PENDING("pending"),
    /**
     * 已变身（有 session）。
     */
    TRANSFORMED("transformed");

    private final String name;

    HenshinState(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    /**
     * 是否处于变身/切形态的进行中（即 PENDING）。
     */
    public boolean isInProgress() {
        return this == PENDING;
    }

    /**
     * 兼容旧存档（"transforming" / "paused"）与未来可能的名字变更。
     * 未知值 fallback 到 IDLE，避免存档加载失败。
     */
    public static final Codec<HenshinState> CODEC = Codec.STRING.xmap(
            s -> switch (s) {
                case "pending", "transforming", "paused" -> PENDING;
                case "transformed" -> TRANSFORMED;
                default -> IDLE;
            },
            HenshinState::getSerializedName
    );
}