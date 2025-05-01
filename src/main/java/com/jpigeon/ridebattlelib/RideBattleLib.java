package com.jpigeon.ridebattlelib;

import com.jpigeon.ridebattlelib.example.ExampleRider;
import com.jpigeon.ridebattlelib.system.RiderRegistry;
import com.jpigeon.ridebattlelib.system.handler.HenshinHandler;
import com.jpigeon.ridebattlelib.system.network.handler.PacketHandler;
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

    private static final Logger LOGGER = LogUtils.getLogger();

    public RideBattleLib(IEventBus modEventBus, ModContainer modContainer)
    {

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(PacketHandler::register);

        NeoForge.EVENT_BUS.register(this);

        NeoForge.EVENT_BUS.register(HenshinHandler.class);

        ExampleRider.init();

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("HELLO FROM COMMON SETUP");


        event.enqueueWork(() -> RiderRegistry.getRegisteredRiders().forEach(config -> {
            if (config.getDriverItem() == null){
                LOGGER.error("骑士 {} 未设置驱动器物品!", config.getRiderId());
            }
        }));

    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {

    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {

    }


    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {

        }
    }



}
