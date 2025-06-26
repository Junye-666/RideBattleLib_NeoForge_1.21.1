package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.core.system.animation.AnimationPhase;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.DriverActionManager;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.HenshinState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

public class AnimationEvent extends Event {
    private final Player player;
    private final ResourceLocation riderId;
    private final ResourceLocation formId;
    private final AnimationPhase phase;
    private boolean canceled = false;

    public AnimationEvent(Player player, ResourceLocation riderId, ResourceLocation formId, AnimationPhase phase) {
        this.player = player;
        this.riderId = riderId;
        this.formId = formId;
        this.phase = phase;
    }

    public Player getPlayer() {
        return player;
    }

    public ResourceLocation getRiderId() {
        return riderId;
    }

    public ResourceLocation getFormId() {
        return formId;
    }

    public AnimationPhase getPhase() {
        return phase;
    }

    public HenshinState getCurrentState() {
        return player.getData(ModAttachments.PLAYER_DATA).getHenshinState();
    }

    public void completeTransformation() {
        DriverActionManager.INSTANCE.completeTransformation(player);
    }

    public boolean isCancelable() {
        return true;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        if (!isCancelable()) {
            throw new UnsupportedOperationException("Attempted to cancel a non-cancelable event");
        }
        this.canceled = canceled;
    }
}
