package com.jpigeon.ridebattlelib.event;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.system.rider.basic.Henshin;
import com.jpigeon.ridebattlelib.system.KeyBindings;
import com.jpigeon.ridebattlelib.system.network.packet.UnhenshinPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
class ClientGameEvents {
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (KeyBindings.UNHENSHIN_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && Henshin.isTransformed(player)) {
                player.connection.send(new UnhenshinPacket());
            }
        }
    }
}
