package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ArmorManager {
    public static final ArmorManager INSTANCE = new ArmorManager();
    // 装备
    public void equipArmor(Player player, FormConfig form) {
        // 先设置通用装备（固定槽位）
        if (form.getHelmet() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(form.getHelmet()));
        }
        if (form.getChestplate() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(form.getChestplate()));
        }
        if (form.getLeggings() != null && form.getLeggings() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(form.getLeggings()));
        }
        if (form.getBoots() != Items.AIR) {
            player.setItemSlot(EquipmentSlot.FEET, new ItemStack(form.getBoots()));
        }

        // 确保盔甲立即生效
        syncEquipment(player);
    }

    // 保存原始装备
    public Map<EquipmentSlot, ItemStack> saveOriginalGear(Player player, RiderConfig config) {
        Map<EquipmentSlot, ItemStack> originalGear = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR ||
                    slot == config.getDriverSlot()) {

                ItemStack stack = player.getItemBySlot(slot);
                // 即使为空也要保存
                originalGear.put(slot, stack.copy());
            }
        }
        return originalGear;
    }

    // 恢复原始装备
    public void restoreOriginalGear(Player player, HenshinSystem.TransformedData data) {
        if (data == null || player == null) return;

        // 恢复所有槽位，包括空槽位
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR &&
                    slot != data.config().getDriverSlot()) {

                ItemStack original = data.originalGear().get(slot);

                // 如果原始装备为空，则清空槽位
                if (original == null || original.isEmpty()) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                } else {
                    player.setItemSlot(slot, original);
                }
            }
        }
        syncEquipment(player);
    }

    // 网络同步功能
    public void syncEquipment(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            List<Pair<EquipmentSlot, ItemStack>> slots = Arrays.stream(EquipmentSlot.values())
                    .map(slot -> {
                        ItemStack stack = player.getItemBySlot(slot);
                        // 确保盔甲耐久度正确显示
                        if (stack.isDamageableItem()) {
                            stack.setDamageValue(0);
                        }
                        return Pair.of(slot, stack);
                    })
                    .toList();

            // 强制同步所有装备槽位
            serverPlayer.connection.send(new ClientboundSetEquipmentPacket(player.getId(), slots));
        }
    }
}
