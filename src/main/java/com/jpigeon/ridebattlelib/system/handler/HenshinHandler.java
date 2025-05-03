package com.jpigeon.ridebattlelib.system.handler;

import com.jpigeon.ridebattlelib.system.rider.basic.Henshin;
import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;
import com.jpigeon.ridebattlelib.system.rider.basic.RiderRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class HenshinHandler {
    //检测玩家右键
    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event){

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
                    Henshin.playerHenshin(player, config);
                    event.setCanceled(true);
                    return;
                }
            }

        }
    }
}
