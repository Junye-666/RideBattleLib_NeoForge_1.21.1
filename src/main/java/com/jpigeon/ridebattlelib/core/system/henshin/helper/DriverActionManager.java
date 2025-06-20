package com.jpigeon.ridebattlelib.core.system.henshin.helper;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.event.HenshinEvent;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SwitchFormPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

public class DriverActionManager {
    public static final DriverActionManager INSTANCE = new DriverActionManager();
    public void preHenshin(Player player, ResourceLocation formId) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;
        HenshinEvent.Pre preHenshin = new HenshinEvent.Pre(player, config.getRiderId(), formId);
        NeoForge.EVENT_BUS.post(preHenshin);

        if (preHenshin.isCanceled()){
            RideBattleLib.LOGGER.info("取消变身");
            return;
        }
    }

    public void proceedHenshin(Player player, RiderConfig config) {
        PacketHandler.sendToServer(new HenshinPacket(config.getRiderId()));
        RideBattleLib.LOGGER.info("发送变身包: {}", config.getRiderId());
    }

    public void proceedFormSwitch(Player player, ResourceLocation newFormId) {
        RideBattleLib.LOGGER.info("发送形态切换包: {}", newFormId);
        PacketHandler.sendToServer(new SwitchFormPacket(newFormId));
    }
}
