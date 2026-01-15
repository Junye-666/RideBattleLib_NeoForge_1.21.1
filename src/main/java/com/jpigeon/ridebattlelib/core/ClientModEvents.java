package com.jpigeon.ridebattlelib.core;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;

import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.TriggerType;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.core.system.network.packet.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RideBattleLib.MODID, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.UNHENSHIN_KEY);
        event.register(KeyBindings.DRIVER_KEY);
        event.register(KeyBindings.RETURN_ITEMS_KEY);
        event.register(KeyBindings.SKILL_KEY);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 初始化客户端缓存
        Minecraft.getInstance().execute(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                HenshinSystem.CLIENT_TRANSFORMED_CACHE.put(player.getUUID(), false);
            }
        });
    }

    private static final Map<UUID, Long> LAST_KEY_PRESS_TIME = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        if (isKeyPressOnCooldown(player)) {
            return;
        }

        setKeyPressCooldown(player);

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

    private static boolean isKeyPressOnCooldown(Player player) {
        Long lastPress = LAST_KEY_PRESS_TIME.get(player.getUUID());
        if (lastPress == null) return false;

        return System.currentTimeMillis() - lastPress < Config.KEY_COOLDOWN_MS.get();
    }

    private static void setKeyPressCooldown(Player player) {
        LAST_KEY_PRESS_TIME.put(player.getUUID(), System.currentTimeMillis());
    }
}
