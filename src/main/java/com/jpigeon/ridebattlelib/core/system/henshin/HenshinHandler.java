package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.core.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.network.packet.HenshinPacket;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;


public class HenshinHandler {
    private static final HenshinSystem HENSHIN_SYSTEM = new HenshinSystem(); // 创建静态实例

    @SubscribeEvent
    public static void onItemRightClick(PlayerInteractEvent.RightClickItem event, RiderConfig config) {
        if (event.getSide() != LogicalSide.CLIENT) return;

        Player player = event.getEntity();

        if (config.isRequiresKeyActivate()){
            return;
        }

            for (RiderConfig riderConfig : RiderRegistry.getRegisteredRiders()) {
                // 检查是否可变身
                if (!HENSHIN_SYSTEM.canTransform(player, riderConfig)) continue;

                // 满足条件则发送变身数据包
                PacketHandler.sendToServer(new HenshinPacket(riderConfig.getRiderId()));
                event.setCanceled(true);
                return;
            }
    }

    // TODO: 添加onKeyPress完成变身按键的检测逻辑

}
