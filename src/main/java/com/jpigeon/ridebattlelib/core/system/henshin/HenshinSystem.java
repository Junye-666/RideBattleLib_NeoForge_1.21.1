package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故事从此开始!
 * 假面骑士的变身系统
 */
public class HenshinSystem implements IHenshinSystem {
    private static final Map<UUID, TransformedData> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();

    public record TransformedData(
            RiderConfig config,
            Map<EquipmentSlot, ItemStack> originalGear
    ) {}

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        RiderConfig config = RiderRegistry.getRider(riderId);
        if (!validateDriver(player, config)) return false;
        if (config == null) return false;

        // 保存原装备（不包括驱动器）
        Map<EquipmentSlot, ItemStack> originalGear = saveOriginalGear(player, config);
        equipArmor(player, config);
        setTransformed(player, config, originalGear);
        return true;
    }

    @Override
    public void unHenshin(Player player) {
        removeTransformed(player);
        TransformedData data = getTransformedData(player);
        if (data != null) {
            restoreOriginalGear(player, data);
            syncEquipment(player);
        }
    }

    //====================变身辅助方法====================

    public void equipArmor(Player player, RiderConfig config){
        // 装备新盔甲
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                Item armorItem = config.getArmorPiece(slot);
                if (armorItem != Items.AIR) {
                    player.setItemSlot(slot, new ItemStack(armorItem));
                }
            }
        }
        // 立即同步装备状态
        syncEquipment(player);
    }

    private void restoreOriginalGear(Player player, TransformedData data) {
        if (data == null || player == null) return;

        // 恢复原装备（不包括驱动器）
        data.originalGear.forEach((slot, stack) -> {
            if (slot != data.config().getDriverSlot()) {
                player.setItemSlot(slot, stack);
            }
        });
    }

    //====================辅助方法====================

    private void syncEquipment(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            List<Pair<EquipmentSlot, ItemStack>> slots = Arrays.stream(EquipmentSlot.values())
                    .map(slot -> Pair.of(slot, player.getItemBySlot(slot)))
                    .toList();
            serverPlayer.connection.send(new ClientboundSetEquipmentPacket(player.getId(), slots));
        }
    }

    public Map<EquipmentSlot, ItemStack> saveOriginalGear(Player player, RiderConfig config) {
        Map<EquipmentSlot, ItemStack> originalGear = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == config.getDriverSlot()) {
                originalGear.put(slot, player.getItemBySlot(slot).copy());
            }
        }
        return originalGear;
    }


    //====================检查方法====================


    @Override
    public boolean isTransformed(Player player) {
        return player != null && TRANSFORMED_PLAYERS.containsKey(player.getUUID());
    }

    //====================Getter方法====================

    @Nullable
    public RiderConfig getConfig(Player player) {
        TransformedData data = getTransformedData(player);
        return data != null ? data.config() : null;
    }

    @Nullable
    public TransformedData getTransformedData(Player player) {
        return player != null ? TRANSFORMED_PLAYERS.get(player.getUUID()) : null;
    }

    //====================Setter方法====================

    public void setTransformed(Player player, RiderConfig config, Map<EquipmentSlot, ItemStack> originalGear) {
        if (player == null || config == null) return;
        TRANSFORMED_PLAYERS.put(player.getUUID(), new TransformedData(config, originalGear));
    }

    public void removeTransformed(Player player) {
        if (player != null) {
            TRANSFORMED_PLAYERS.remove(player.getUUID());
        }
    }

}
