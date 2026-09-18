package com.jpigeon.ridebattlelib.common.api.server;

import com.jpigeon.ridebattlelib.server.event.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface IRiderServerHandler {
    ResourceLocation riderId();

    /**
     * 变身
     */
    default void onHenshinPre(@NotNull HenshinEvent.Pre event) {
    }

    default void onHenshinPost(@NotNull HenshinEvent.Post event) {
    }

    /**
     * 形态切换
     */
    default void onSwitchPre(@NotNull FormSwitchEvent.Pre event) {
    }

    default void onSwitchPost(@NotNull FormSwitchEvent.Post event) {
    }

    /**
     * 解除变身
     */
    default void onUnhenshinPre(@NotNull UnhenshinEvent.Pre event) {
    }

    default void onUnhenshinPost(@NotNull UnhenshinEvent.Post event) {
    }

    /**
     * 解除变身（仅Post事件）
     */
    default void onInsert(@NotNull ItemInsertionEvent.Post event) {
    }

    default void onExtract(@NotNull SlotExtractionEvent.Post event) {
    }
}
