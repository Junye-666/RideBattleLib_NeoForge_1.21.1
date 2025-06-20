package com.jpigeon.ridebattlelib.core.system.henshin;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.KeyBindings;

import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.HenshinPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.ReturnItemsPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.SwitchFormPacket;
import com.jpigeon.ridebattlelib.core.system.network.packet.UnhenshinPacket;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import java.util.Map;

public class HenshinHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (KeyBindings.DRIVER_KEY.consumeClick()) {
            RiderConfig config = RiderConfig.findActiveDriverConfig(player);
            if (config == null) return;
            RideBattleLib.LOGGER.info("按键触发 - 玩家状态: 变身={}, 驱动器={}", HenshinSystem.INSTANCE.isTransformed(player), config.getRiderId());
        }

        if (KeyBindings.UNHENSHIN_KEY.consumeClick()) {
            RideBattleLib.LOGGER.debug("发送解除变身数据包");
            PacketHandler.sendToServer(new UnhenshinPacket());
        }

        if (KeyBindings.RETURN_ITEMS_KEY.consumeClick()) {
            // 触发物品返还
            PacketHandler.sendToServer(new ReturnItemsPacket());
        }
    }
}