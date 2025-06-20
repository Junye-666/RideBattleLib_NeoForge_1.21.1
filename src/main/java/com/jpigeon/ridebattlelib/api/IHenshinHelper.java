package com.jpigeon.ridebattlelib.api;

import com.jpigeon.ridebattlelib.core.system.attachment.TransformedAttachmentData;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IHenshinHelper {
    void performHenshin(Player player, RiderConfig config, ResourceLocation formId);
    void performFormSwitch(Player player, ResourceLocation newFormId);
    void restoreTransformedState(Player player, TransformedAttachmentData data);
    void setTransformed(Player player, RiderConfig config, ResourceLocation formId, Map<EquipmentSlot, ItemStack> originalGear, Map<ResourceLocation, ItemStack> beltSnapshot);
    void removeTransformed(Player player);
}
