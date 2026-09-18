package com.jpigeon.ridebattlelib.common.api.registry;

import com.jpigeon.ridebattlelib.RideBattleLib;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RiderPackRegistry {
    private static final Map<ResourceLocation, IRiderPack> PACKS = new ConcurrentHashMap<>();
    private static boolean commonInitialized = false;
    private static boolean clientInitialized = false;

    public static void register(IRiderPack pack) {
        if (PACKS.putIfAbsent(pack.riderId(), pack) != null) {
            RideBattleLib.LOGGER.warn("重复注册骑士包: {}", pack.riderId());
            return;
        }
        RideBattleLib.LOGGER.info("注册骑士包: {}", pack.riderId());
    }

    public static Collection<IRiderPack> all() {
        return Collections.unmodifiableCollection(PACKS.values());
    }

    /**
     * 双端都会调用一次（在 FMLCommonSetupEvent 中，enqueueWork 后）
     */
    public static void initCommon() {
        if (commonInitialized) return;
        commonInitialized = true;
        PACKS.values().forEach(IRiderPack::registerCommon);
    }

    /**
     * 仅物理客户端调用（FMLClientSetupEvent）
     */
    public static void initClient() {
        if (clientInitialized) return;
        if (!FMLEnvironment.dist.isClient()) return;
        clientInitialized = true;
        PACKS.values().forEach(IRiderPack::registerClient);
    }
}
