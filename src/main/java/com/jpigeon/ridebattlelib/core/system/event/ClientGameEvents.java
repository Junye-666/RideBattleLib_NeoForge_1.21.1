package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.KeyBindings;
import com.jpigeon.ridebattlelib.core.network.packet.UnhenshinPacket;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = RideBattleLib.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
class ClientGameEvents {
    private static final HenshinSystem HENSHIN_SYSTEM = new HenshinSystem();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event, RiderConfig config) {
        while (KeyBindings.UNHENSHIN_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && HENSHIN_SYSTEM.isTransformed(player)) {  // 修改这里
                PacketHandler.sendToServer(new UnhenshinPacket());
            }
        }
        while (KeyBindings.HENSHIN_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && !HENSHIN_SYSTEM.isTransformed(player)){
                ResourceLocation riderId = config.getRiderId();
                PacketHandler.sendToServer(new HenshinPacket(riderId));
            }
        }
    }
}
