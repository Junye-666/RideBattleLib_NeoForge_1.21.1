package com.jpigeon.ridebattlelib;

import com.jpigeon.ridebattlelib.core.system.belt.BeltHandler;
import com.jpigeon.ridebattlelib.core.system.belt.BeltSystem;
import com.jpigeon.ridebattlelib.core.system.form.FormConfig;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinHandler;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import com.jpigeon.ridebattlelib.core.system.henshin.TriggerItemHandler;
import com.jpigeon.ridebattlelib.example.ExampleRiders;
import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.core.network.handler.PacketHandler;
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
        public static void onClientSetup(FMLClientSetupEvent event)
        {
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // 加载数据前记录状态
        RideBattleLib.LOGGER.debug("玩家登录: {}", player.getName());
        RideBattleLib.LOGGER.debug("登录前腰带数据: {}", BeltSystem.INSTANCE.getBeltItems(player));

        // 加载数据
        BeltSystem.INSTANCE.loadBeltData(player);
        HenshinSystem.INSTANCE.loadTransformedState(player);

        // 加载后记录状态
        RideBattleLib.LOGGER.debug("登录后腰带数据: {}", BeltSystem.INSTANCE.getBeltItems(player));

        // 同步数据
        BeltSystem.INSTANCE.syncBeltData(player);

        // 不需要重新应用属性，只需确保盔甲正确
        if (HenshinSystem.INSTANCE.isTransformed(player)) {
            HenshinSystem.TransformedData data = HenshinSystem.INSTANCE.getTransformedData(player);
            if (data != null) {
                FormConfig form = data.config().getForm(data.formId());
                if (form != null) {
                    // 确保盔甲装备正确
                    HenshinSystem.INSTANCE.equipArmor(player, form);

                    HenshinSystem.INSTANCE.applyAttributes(player, form);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();

        // 保存腰带数据
        BeltSystem.INSTANCE.saveBeltData(player);

        // 保存变身状态
        HenshinSystem.INSTANCE.saveTransformedState(player);
    }
}
