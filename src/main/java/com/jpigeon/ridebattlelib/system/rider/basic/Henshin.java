package com.jpigeon.ridebattlelib.system.rider.basic;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 故事从此开始!
 * 假面骑士的变身系统
 */
public class Henshin {
    private static final Map<UUID, RiderConfig> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();

    //变身逻辑
    public static void playerHenshin(Player player, RiderConfig config){
        if (!canTransform(player, config)){
            return;
        }

        ItemStack originalLeggings = player.getItemBySlot(EquipmentSlot.LEGS).copy();


        if(config.getHelmet() == null || config.getChestplate() == null || config.getBoots() == null){
            throw new IllegalStateException("骑士" + config.getRiderId() + "配置不完整");
        }

        //装备盔甲
        safeSetArmor(player, EquipmentSlot.HEAD, config.getHelmet());
        safeSetArmor(player, EquipmentSlot.CHEST, config.getChestplate());
        conditionalEquipLeggings(player, config.getLeggings(), originalLeggings);
        safeSetArmor(player, EquipmentSlot.FEET, config.getBoots());

        setTransformed(player, config);
        //...待后续添加其它变身时触发
    }

    //解除变身逻辑
    public static void playerUnhenshin(Player player){
        if(player == null || !player.isAlive()) return;

        removeTransformed(player).ifPresent(config -> {
            // 根据 config 清理装备
            clearArmor(player, config);


        });
    }



    //====================变身辅助方法====================

    //安全设置盔甲
    public static void safeSetArmor(Player player, EquipmentSlot slot, @Nullable Item item){
        ItemStack stack = (item != null && item != Items.AIR) ?
                new ItemStack(item) : ItemStack.EMPTY;
        player.setItemSlot(slot, stack);
    }

    public static void conditionalEquipLeggings(Player player, @Nullable Item leggings, ItemStack original){
        if (leggings != null && leggings != Items.AIR){
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(leggings));
        } else {
            player.setItemSlot(EquipmentSlot.LEGS, original);
        }
    }

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
        return TRANSFORMED_PLAYERS.containsKey(player.getUUID());
    }

    private static boolean canTransform(Player player, RiderConfig config) {
        // 在player或config不存在时, 玩家以变身时返回false
        if (player == null || config == null || isTransformed(player)){
            return false;
        }

        ItemStack legItem = player.getItemBySlot(EquipmentSlot.LEGS);

        //返回
        return !legItem.isEmpty()   //腿部不为空
                && config.getDriverItem() != null   //配置中的驱动器不为空
                && legItem.is(config.getDriverItem())  //腿部物品等于配置驱动器物品


    ;}

    //====================Setter方法====================

    public static synchronized void setTransformed(Player player, RiderConfig config) {
        TRANSFORMED_PLAYERS.put(player.getUUID(), config);
    }

    public static synchronized Optional<RiderConfig> removeTransformed(Player player) {
        return Optional.ofNullable(player != null ? TRANSFORMED_PLAYERS.remove(player.getUUID()) : null);
    }

    //====================Getter方法====================

    public static RiderConfig getConfig(Player player) {
        return TRANSFORMED_PLAYERS.get(player.getUUID());
    }

}