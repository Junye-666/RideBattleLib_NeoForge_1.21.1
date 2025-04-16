package com.jpigeon.ridebattlelib.system;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

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
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(config.getHelmet()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(config.getChestplate()));
        if (config.getLeggings() != null) {
            player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(config.getLeggings()));
        }
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(config.getBoots()));


        //...待后续添加
    }
}