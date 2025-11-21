package com.jpigeon.ridebattlelib;

import com.jpigeon.ridebattlelib.core.system.henshin.RiderRegistry;
import com.jpigeon.ridebattlelib.example.ExampleBasic;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = RideBattleLib.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = RideBattleLib.MODID, value = Dist.CLIENT)
public class RideBattleLibClient {
    public RideBattleLibClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        RideBattleLib.LOGGER.info("请确保骑士初始化在ClientSetup中哦~");
        ExampleBasic.init();
        // ExampleDynamicForm.init();

        event.enqueueWork(() -> RiderRegistry.getRegisteredRiders().forEach(config -> {
            if (config.getDriverItem() == null) {
                RideBattleLib.LOGGER.error("骑士 {} 未设置驱动器物品!", config.getRiderId());
            }
        }));
    }
}
