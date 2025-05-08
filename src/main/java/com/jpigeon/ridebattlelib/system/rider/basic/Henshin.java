package com.jpigeon.ridebattlelib.system.rider.basic;

import com.mojang.datafixers.util.Pair;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
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
public class Henshin {
    private static final Map<UUID, TransformedData> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();

    private record TransformedData(
            RiderConfig config,
            Map<EquipmentSlot, ItemStack> originalGear
    ) {}

    //变身逻辑
    public static void playerHenshin(Player player, RiderConfig config) {
        if (!canTransform(player, config)) return;

        // 保存原装备（不包括驱动器）
        Map<EquipmentSlot, ItemStack> originalGear = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == config.getDriverSlot()) {
                originalGear.put(slot, player.getItemBySlot(slot).copy());
            }
        }

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
        TRANSFORMED_PLAYERS.put(player.getUUID(), new TransformedData(config, originalGear));
    }

    //解除变身逻辑
    public static void playerUnhenshin(Player player) {
        TransformedData data = TRANSFORMED_PLAYERS.remove(player.getUUID());
        if (data == null || player == null) return;

        // 恢复原装备（不包括驱动器）
        data.originalGear.forEach((slot, stack) -> {
            if (slot != data.config.getDriverSlot()) {
                player.setItemSlot(slot, stack);
            }
        });

        syncEquipment(player);
    }



    //====================变身辅助方法====================

    public static void clearArmor(Player player, RiderConfig config){
        //根据配置清除装备
        if(config.getHelmet() != null) {
            safeClearSlot(player, EquipmentSlot.HEAD, config.getHelmet());
        }
        if(config.getChestplate() != null) {
            safeClearSlot(player, EquipmentSlot.CHEST, config.getChestplate());
        }
        //腿部保留驱动器
        if(config.getBoots() != null) {
            safeClearSlot(player, EquipmentSlot.FEET, config.getBoots());
        }

    }

        //匹配配置
    public static void safeClearSlot(Player player, EquipmentSlot slot, Item expectedItem){
        ItemStack current = player.getItemBySlot(slot);
        if (!current.isEmpty() && expectedItem != null && current.is(expectedItem)) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    //====================检查方法====================

    public static boolean isTransformed(Player player){
        //在TRANSFORMED_PLAYERS列表中寻找玩家ID
        return player != null && TRANSFORMED_PLAYERS.containsKey(player.getUUID());
    }

    static boolean canTransform(Player player, RiderConfig config) {
        if (player == null || config == null || isTransformed(player)) {
            return false;
        }

        // 检查驱动器物品
        boolean hasDriver = player.getItemBySlot(config.getDriverSlot()).is(config.getDriverItem());

        // 检查需求物品（手持任意一只手）
        boolean hasRequiredItem = config.getRequiredItem() == Items.AIR ||
                player.getMainHandItem().is(config.getRequiredItem()) ||
                player.getOffhandItem().is(config.getRequiredItem());

        return hasDriver && hasRequiredItem;
    }

    //====================Setter方法====================

    public static void setTransformed(Player player, RiderConfig config, Map<EquipmentSlot, ItemStack> originalGear) {
        if (player == null || config == null) return;
        TRANSFORMED_PLAYERS.put(player.getUUID(), new TransformedData(config, originalGear));
    }

    public static Optional<TransformedData> removeTransformed(Player player) {
        return Optional.ofNullable(player != null ? TRANSFORMED_PLAYERS.remove(player.getUUID()) : null);
    }

    //====================Getter方法====================

    @Nullable
    public static RiderConfig getConfig(Player player) {
        TransformedData data = getTransformedData(player);
        return data != null ? data.config() : null;
    }

    @Nullable
    public static TransformedData getTransformedData(Player player) {
        return player != null ? TRANSFORMED_PLAYERS.get(player.getUUID()) : null;
    }

    //====================辅助方法====================

    private static void syncEquipment(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            List<Pair<EquipmentSlot, ItemStack>> slots = Arrays.stream(EquipmentSlot.values())
                    .map(slot -> Pair.of(slot, player.getItemBySlot(slot)))
                    .toList();
            serverPlayer.connection.send(new ClientboundSetEquipmentPacket(player.getId(), slots));
        }
    }
}
