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
import net.minecraft.world.entity.player.Player;
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

        if (KeyBindings.HENSHIN_KEY.consumeClick()) {
            RiderConfig activeConfig = RiderConfig.findActiveDriverConfig(player);
            if (activeConfig != null) {
                if (HenshinSystem.INSTANCE.isTransformed(player)) {
                    if (activeConfig.getTriggerType() == TriggerType.KEY) {
                        Map<ResourceLocation, ItemStack> beltItems = BeltSystem.INSTANCE.getBeltItems(player);
                        ResourceLocation newFormId = activeConfig.matchForm(beltItems);

                        // 添加调试日志
                        RideBattleLib.LOGGER.info("发送形态切换包: {}", newFormId);
                        PacketHandler.sendToServer(new SwitchFormPacket(newFormId));
                    }
                } else if (activeConfig.getTriggerType() == TriggerType.KEY) {
                    PacketHandler.sendToServer(new HenshinPacket(activeConfig.getRiderId()));
                }
            }
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

    public static void handleFormSwitch(Player player, ResourceLocation newFormId) {
        // 这个方法现在只作为通用模板，实际不会被直接调用
        RideBattleLib.LOGGER.warn("通用handleFormSwitch被调用，应使用类型专用方法");
    }

    public static void handleKeyFormSwitch(Player player, ResourceLocation newFormId) {
        RideBattleLib.LOGGER.info("处理KEY类型形态切换: {}", newFormId);
        HenshinSystem.INSTANCE.performFormSwitch(player, newFormId);
    }
}