package com.jpigeon.ridebattlelib.core.system.event;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.KeyBindings;

import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;


@EventBusSubscriber(modid = RideBattleLib.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.UNHENSHIN_KEY);
        event.register(KeyBindings.HENSHIN_KEY);
        event.register(KeyBindings.RETURN_ITEMS_KEY);
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
}

