package com.jpigeon.ridebattlelib;

import com.jpigeon.ridebattlelib.core.system.belt.BeltHandler;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinHandler;
import com.jpigeon.ridebattlelib.core.system.henshin.TriggerItemHandler;
import com.jpigeon.ridebattlelib.example.ExampleRiders;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;


import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(RideBattleLib.MODID)
public class RideBattleLib
{
    public static final String MODID = "ridebattlelib";

    public static final Logger LOGGER = LogUtils.getLogger();

    public RideBattleLib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(PacketHandler::register);

        NeoForge.EVENT_BUS.register(this);

        NeoForge.EVENT_BUS.register(HenshinHandler.class);
        NeoForge.EVENT_BUS.register(BeltHandler.class);
        NeoForge.EVENT_BUS.register(TriggerItemHandler.class);

        ExampleRiders.init();

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::initAttachments);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");


        event.enqueueWork(() -> RiderRegistry.getRegisteredRiders().forEach(config -> {
            if (config.getDriverItem() == null){
                LOGGER.error("骑士 {} 未设置驱动器物品!", config.getRiderId());
            }
        }));

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        BeltSystem.INSTANCE.syncBeltData(player);
    }

    public static final AttachmentType<PlayerPersistentData> PLAYER_DATA =
            AttachmentType.serializable(() -> PlayerPersistentData.CODEC)
                    .build();

    public static void initAttachments(RegisterDataAttachmentTypesEvent event) {
        event.register(PLAYER_DATA);
    }
}
