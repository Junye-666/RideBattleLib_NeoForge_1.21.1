package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.mojang.serialization.Codec;

import java.util.HashMap;
import java.util.Map;

public enum HenshinState {
    IDLE("idle"),           // 空闲状态
    TRANSFORMING("transforming"), // 变身中（仅用于有暂停的变身）
    TRANSFORMED("transformed");   // 已变身

    private final String id;
    private static final Map<String, HenshinState> BY_ID = new HashMap<>();

    static {
        for (HenshinState state : values()) {
            BY_ID.put(state.id, state);
        }
    }

    HenshinState(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static HenshinState byId(String id) {
        return BY_ID.getOrDefault(id, IDLE);
    }

    // 编解码器支持
    public static final Codec<HenshinState> CODEC = Codec.STRING.xmap(
            HenshinState::byId,
            HenshinState::getId
    );
}
