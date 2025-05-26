package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IHenshinSystem;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
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
    public static final HenshinSystem INSTANCE = new HenshinSystem();

    public record TransformedData(
            RiderConfig config,
            Map<EquipmentSlot, ItemStack> originalGear
    ) {
    }

    @Override
    public boolean henshin(Player player, ResourceLocation riderId) {
        if (player.level().isClientSide()) {
            RideBattleLib.LOGGER.error("变身逻辑不能在客户端执行");
            return false;
        }

        if (isTransformed(player)) {
            RideBattleLib.LOGGER.warn("玩家已处于变身状态");
            return false;
        }

        RiderConfig config = RiderRegistry.getRider(riderId);
        if (config == null) {
            RideBattleLib.LOGGER.error("未找到骑士配置: {}", riderId);
            return false;
        }

        // 验证驱动器
        if (!validateDriver(player, config)) {
            RideBattleLib.LOGGER.warn("驱动器验证失败: 玩家未穿戴 {} 或槽位错误", config.getDriverItem());
            return false;
        }

        // 验证腰带物品
        if (!BeltSystem.INSTANCE.validateItems(player, riderId)) {
            RideBattleLib.LOGGER.warn("槽位验证失败: 必要槽位未正确填充");
            player.displayClientMessage(Component.translatable("ridebattlelib.validateItems.fail"), true);
            return false;
        }

        // 检查是否已变身
        if (isTransformed(player)) {
            RideBattleLib.LOGGER.warn("玩家已处于变身状态");
            return false;
        }

        // 检查驱动器
        ItemStack driverStack = player.getItemBySlot(config.getDriverSlot());
        boolean isDriverValid = driverStack.is(config.getDriverItem());
        RideBattleLib.LOGGER.info("驱动器验证: 槽位 {} | 物品 {} | 有效: {}",
                config.getDriverSlot(), driverStack.getItem(), isDriverValid
        );
        if (!isDriverValid) {
            return false;
        }

        // 执行变身逻辑
        RideBattleLib.LOGGER.info("玩家 {} 成功变身为 {}", player.getName(), riderId);
        Map<EquipmentSlot, ItemStack> originalGear = saveOriginalGear(player, config);
        equipArmor(player, config);
        setTransformed(player, config, originalGear);

        // 日志输出
        RideBattleLib.LOGGER.info("玩家 {} 变身为 {}，腰带数据: {}",
                player.getName(),
                riderId,
                BeltSystem.INSTANCE.getBeltItems(player)
        );

        return true;
    }

    @Override
    public void unHenshin(Player player) {
        TransformedData data = getTransformedData(player);
        if (data != null) {
            restoreOriginalGear(player, data);
            syncEquipment(player);
            removeTransformed(player);
            BeltSystem.INSTANCE.returnItems(player);
            RideBattleLib.LOGGER.debug("解除变身并返还物品: {}", player.getName());
        }
    }

    //====================变身辅助方法====================

    public void equipArmor(Player player, RiderConfig config) {
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
