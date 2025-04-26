package com.jpigeon.ridebattlelib.system;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

public class Henshin {
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event){  //检测玩家右键
        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();

        for (RiderConfig config : RiderRegistry.getRegisteredRiders()){
            //检查是否手持变身所需物品
            if(config.getRequiredItem() != null &&
                    heldItem.getItem() == config.getRequiredItem()){

                //检查玩家是否佩戴驱动器
                boolean hasDriver = player.getItemBySlot(config.getDriverSlot()).getItem() == config.getDriverItem();

                //呼叫变身进程
                if(hasDriver){
                    playerHenshin(player, config);
                    event.setCanceled(true);
                    return;
                }
            }

        }
    }

    public static void playerHenshin(Player player, RiderConfig config){    //变身逻辑
        //装备盔甲
        safeSetArmor(player, EquipmentSlot.HEAD, config.getHelmet());
        safeSetArmor(player, EquipmentSlot.CHEST, config.getChestplate());
        safeSetArmor(player, EquipmentSlot.LEGS, config.getLeggings());
        safeSetArmor(player, EquipmentSlot.FEET, config.getBoots());


        //...待后续添加其它变身时触发
    }

    //安全设置盔甲
    public static void safeSetArmor(Player player, EquipmentSlot slot, @Nullable Item item){
        if(item != null && item != Items.AIR){
            player.setItemSlot(slot, new ItemStack(item));
        } else {
            player.setItemSlot(slot, ItemStack.EMPTY); //明确空槽位
        }
    }
}