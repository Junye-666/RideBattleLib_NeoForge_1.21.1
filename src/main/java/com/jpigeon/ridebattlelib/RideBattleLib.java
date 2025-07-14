package com.jpigeon.ridebattlelib;

import com.jpigeon.ridebattlelib.core.system.attachment.AttachmentHandler;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.belt.BeltHandler;
import com.jpigeon.ridebattlelib.core.system.henshin.*;
import com.jpigeon.ridebattlelib.core.system.henshin.handler.KeyHandler;
import com.jpigeon.ridebattlelib.core.system.henshin.handler.TriggerItemHandler;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltyHandler;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import com.jpigeon.ridebattlelib.example.ExampleBasic;
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
public class RideBattleLib {
    public static final String MODID = "ridebattlelib";

    public static final Logger LOGGER = LogUtils.getLogger();

    public RideBattleLib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(PacketHandler::register);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(KeyHandler.class);
        NeoForge.EVENT_BUS.register(BeltHandler.class);
        NeoForge.EVENT_BUS.register(TriggerItemHandler.class);
        NeoForge.EVENT_BUS.register(AttachmentHandler.class);
        NeoForge.EVENT_BUS.register(PenaltyHandler.class);
        RiderAttachments.ATTACHMENTS.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
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
            LOGGER.info("请确保骑士初始化在ClientSetup中哦~");
            ExampleBasic.init();
            // ExampleDynamicForm.init();

            event.enqueueWork(() -> RiderRegistry.getRegisteredRiders().forEach(config -> {
                if (config.getDriverItem() == null) {
                    LOGGER.error("骑士 {} 未设置驱动器物品!", config.getRiderId());
                }
            }));
        }
    }
}
