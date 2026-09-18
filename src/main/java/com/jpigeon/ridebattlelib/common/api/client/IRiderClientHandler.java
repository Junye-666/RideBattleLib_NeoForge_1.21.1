package com.jpigeon.ridebattlelib.common.api.client;

import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.server.event.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 骑士的客户端处理器。
 * <p>
 * 每个 {@link RiderConfig} 对应一个
 * {@code IRiderClientHandler}，在 {@code FMLClientSetupEvent} 里通过
 * {@link ClientRiderDispatcher#register(IRiderClientHandler)} 注册。
 *
 * @see ClientRiderContext
 *
 */
public interface IRiderClientHandler {
    ResourceLocation riderId();

    /**
     * 对应 {@link HenshinEvent.Post}
     */
    default void postHenshin(@NotNull ClientRiderContext ctx) {
    }

    /**
     * 对应 {@link FormSwitchEvent.Post}
     */
    default void postSwitch(@NotNull ClientRiderContext ctx) {
    }

    /**
     * 对应 {@link UnhenshinEvent.Post}
     */
    default void postUnhenshin(@NotNull ClientRiderContext ctx) {
    }

    /**
     * 对应 {@link HenshinEvent.Pre} / {@link FormSwitchEvent.Pre}
     */
    default void onPending(@NotNull ClientRiderContext ctx) {
    }

    /**
     * 对应 {@link ItemInsertionEvent.Post}
     */
    default void onDriverItemInserted(@NotNull ClientRiderContext ctx) {
    }

    /**
     * 对应 {@link SlotExtractionEvent.Post}
     */
    default void onDriverItemExtracted(@NotNull ClientRiderContext ctx) {
    }

    /**
     * 对应 {@link SkillEvent.Post}
     */
    default void onSkill(@NotNull ClientRiderContext ctx) {
    }
}
