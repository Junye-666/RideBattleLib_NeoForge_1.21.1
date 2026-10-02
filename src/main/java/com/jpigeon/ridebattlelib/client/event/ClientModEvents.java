package com.jpigeon.ridebattlelib.client.event;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.client.cache.ClientDriverDataCache;
import com.jpigeon.ridebattlelib.client.cache.ClientTransformedCache;
import com.jpigeon.ridebattlelib.client.key.KeyBindings;
import com.jpigeon.ridebattlelib.common.api.RideBattleAPI;
import com.jpigeon.ridebattlelib.common.network.packet.*;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import net.minecraft.client.KeyMapping;
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
    private static final Map<UUID, Map<KeyMapping, Long>> LAST_PRESS = new ConcurrentHashMap<>();

    private static boolean onCooldown(Player p, KeyMapping key) {
        var m = LAST_PRESS.get(p.getUUID());
        if (m == null) return false;
        Long t = m.get(key);
        return t != null && System.currentTimeMillis() - t < Config.KEY_COOLDOWN_MS.get();
    }

    private static void markPressed(Player p, KeyMapping key) {
        LAST_PRESS.computeIfAbsent(p.getUUID(), k -> new ConcurrentHashMap<>())
                .put(key, System.currentTimeMillis());
    }

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
            LAST_PRESS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        // 驱动器键
        if (KeyBindings.DRIVER_KEY.consumeClick()
                && !onCooldown(player, KeyBindings.DRIVER_KEY)) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("按键触发 - 玩家状态: 变身={}",
                        HenshinUtils.isTransformed(player));
            }
            PacketDistributor.sendToServer(DriverActionPacket.INSTANCE);
            markPressed(player, KeyBindings.DRIVER_KEY);
        }

        // 解除变身键
        if (KeyBindings.UNHENSHIN_KEY.consumeClick()
                && !onCooldown(player, KeyBindings.UNHENSHIN_KEY)) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("发送解除变身数据包");
            }
            PacketDistributor.sendToServer(UnhenshinPacket.INSTANCE);
            markPressed(player, KeyBindings.UNHENSHIN_KEY);
        }

        // 物品返还键
        if (KeyBindings.RETURN_ITEMS_KEY.consumeClick()
                && !onCooldown(player, KeyBindings.RETURN_ITEMS_KEY)) {
            PacketDistributor.sendToServer(ReturnItemsPacket.INSTANCE);
            markPressed(player, KeyBindings.RETURN_ITEMS_KEY);
        }

        // 技能键
        if (KeyBindings.SKILL_KEY.consumeClick()
                && !onCooldown(player, KeyBindings.SKILL_KEY)) {
            if (Config.DEBUG_MODE.get()) {
                RideBattleLib.LOGGER.debug("检测到技能键按下");
            }
            if (RideBattleAPI.isTransformed(player)) {
                if (player.isShiftKeyDown()) {
                    PacketDistributor.sendToServer(RotateSkillPacket.INSTANCE);
                } else {
                    PacketDistributor.sendToServer(TriggerSkillPacket.INSTANCE);
                }
                markPressed(player, KeyBindings.SKILL_KEY);
            }
        }
    }
}
