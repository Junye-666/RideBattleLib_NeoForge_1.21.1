package com.jpigeon.ridebattlelib.system.network.handler;

import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.system.rider.basic.Henshin;
import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;

import com.jpigeon.ridebattlelib.system.network.packet.UnhenshinPacket;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;



public class PacketHandler {
    public static void register(final RegisterPayloadHandlersEvent event) {
        event.registrar(RideBattleLib.MODID)
                .versioned("1.0.0")
                .playToServer(
                        UnhenshinPacket.TYPE,
                        UnhenshinPacket.STREAM_CODEC,
                        (payload, context) -> {
                            // 直接获取 Player 对象（可能为 null）
                            Player player = context.player();

                            // 手动进行 null 检查
                            if (player != null) {
                                RiderConfig config = Henshin.getConfig(player);
                                if (config != null) {
                                    Henshin.playerUnhenshin(player);
                                }
                            }
                        }
                );
    }
}
