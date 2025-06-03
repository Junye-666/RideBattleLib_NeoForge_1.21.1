package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IHenshinHelper {
    void executeTransform(Player player, RiderConfig config, ResourceLocation formId);
    void executeFormSwitch(Player player, ResourceLocation newFormId);
    void restoreTransformedState(Player player, TransformedAttachmentData data);
}
