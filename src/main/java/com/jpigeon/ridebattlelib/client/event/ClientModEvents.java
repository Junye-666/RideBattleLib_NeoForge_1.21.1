package com.jpigeon.ridebattlelib.client.event;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.client.cache.ClientDriverDataCache;
import com.jpigeon.ridebattlelib.client.cache.ClientTransformedCache;
import com.jpigeon.ridebattlelib.client.key.KeyBindings;
import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.network.packet.*;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LocalPlayer player = event.getPlayer();
        if (player != null) {
            ClientTransformedCache.remove(player.getUUID());
            ClientDriverDataCache.remove(player.getUUID());
        }
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

            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("按键触发 - 玩家状态: 变身={}, 驱动器={}", HenshinUtils.isTransformed(player), config.getRiderId());
            }
            PacketDistributor.sendToServer(new DriverActionPacket(player.getUUID()));

        }
        if (KeyBindings.UNHENSHIN_KEY.consumeClick()) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("发送解除变身数据包");
            }
            PacketDistributor.sendToServer(new UnhenshinPacket(player.getUUID()));
        }

        if (KeyBindings.RETURN_ITEMS_KEY.consumeClick()) {
            // 触发物品返还
            PacketDistributor.sendToServer(new ReturnItemsPacket());
        }

        if (KeyBindings.SKILL_KEY.consumeClick()) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("检测到技能键按下");
            }
            if (!ClientTransformedCache.isTransformed(player.getUUID())) return;
            // 蹲下时切换技能，否则触发当前技能
            if (player.isShiftKeyDown()) {
                PacketDistributor.sendToServer(new RotateSkillPacket(player.getUUID()));
            } else {
                PacketDistributor.sendToServer(new TriggerSkillPacket(player.getUUID()));
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
