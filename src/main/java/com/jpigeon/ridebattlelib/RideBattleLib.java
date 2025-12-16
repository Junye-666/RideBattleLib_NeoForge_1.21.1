package com.jpigeon.ridebattlelib;

import com.jpigeon.ridebattlelib.core.system.attachment.AttachmentHandler;
import com.jpigeon.ridebattlelib.core.system.attachment.RiderAttachments;
import com.jpigeon.ridebattlelib.core.system.driver.DriverHandler;
import com.jpigeon.ridebattlelib.core.system.henshin.helper.CountdownManager;
import com.jpigeon.ridebattlelib.core.system.penalty.PenaltyHandler;
import com.jpigeon.ridebattlelib.core.system.network.handler.PacketHandler;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RideBattleLib.MODID)
public class RideBattleLib {
    public static final String MODID = "ridebattlelib";

    public static final Logger LOGGER = LogUtils.getLogger();

    public RideBattleLib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(PacketHandler::register);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(DriverHandler.class);
        NeoForge.EVENT_BUS.register(AttachmentHandler.class);
        NeoForge.EVENT_BUS.register(PenaltyHandler.class);
        NeoForge.EVENT_BUS.register(CountdownManager.getInstance());
        RiderAttachments.ATTACHMENTS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
