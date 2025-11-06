package com.jpigeon.ridebattlelib.core.system.henshin.handler;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.KeyBindings;

import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;


public class KeyHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (KeyBindings.DRIVER_KEY.consumeClick()) {
            RiderConfig config = RiderConfig.findActiveDriverConfig(player);
            if (config == null) return;

            FormConfig formConfig = config.getActiveFormConfig(player);
            if (formConfig != null && formConfig.getTriggerType() == TriggerType.KEY) {
                if (Config.DEBUG_MODE.get()) {
                    RideBattleLib.LOGGER.debug("按键触发 - 玩家状态: 变身={}, 驱动器={}", HenshinSystem.INSTANCE.isTransformed(player), config.getRiderId());
                }
                PacketHandler.sendToServer(new DriverActionPacket(player.getUUID()));
            }
        }
        if (KeyBindings.UNHENSHIN_KEY.consumeClick()) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("发送解除变身数据包");
            }
            PacketHandler.sendToServer(new UnhenshinPacket(player.getUUID()));
        }

        if (KeyBindings.RETURN_ITEMS_KEY.consumeClick()) {
            // 触发物品返还
            PacketHandler.sendToServer(new ReturnItemsPacket());
        }

        if (KeyBindings.SKILL_KEY.consumeClick()) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("检测到技能键按下");
            }
            // 蹲下时切换技能，否则触发当前技能
            if (player.isShiftKeyDown()) {
                if (!HenshinSystem.INSTANCE.isTransformed(player)) return;
                PacketHandler.sendToServer(new RotateSkillPacket(player.getUUID()));
            } else {
                if (!HenshinSystem.INSTANCE.isTransformed(player)) return;
                PacketHandler.sendToServer(new TriggerSkillPacket(player.getUUID()));
            }
        }
    }
}
