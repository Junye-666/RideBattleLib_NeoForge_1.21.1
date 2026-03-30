package com.jpigeon.ridebattlelib;

import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.network.PacketHandler;
import com.jpigeon.ridebattlelib.common.util.ScheduleUtils;
import com.jpigeon.ridebattlelib.server.handler.AttachmentHandler;
import com.jpigeon.ridebattlelib.server.handler.DriverHandler;
import com.jpigeon.ridebattlelib.server.handler.PenaltyHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(RideBattleLib.MODID)
public class RideBattleLib {
    public static final String MODID = "ridebattlelib";

    public static final Logger LOGGER = LogUtils.getLogger();

    public RideBattleLib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(PacketHandler::register);

        NeoForge.EVENT_BUS.register(DriverHandler.class);
        NeoForge.EVENT_BUS.register(AttachmentHandler.class);
        NeoForge.EVENT_BUS.register(PenaltyHandler.class);
        NeoForge.EVENT_BUS.register(ScheduleUtils.getInstance());
        RiderAttachments.ATTACHMENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
