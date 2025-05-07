package com.jpigeon.ridebattlelib.system.rider.basic;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 理解为管理所有被注册骑士的列表
 */

public class RiderRegistry {
    public static final Map<ResourceLocation, RiderConfig> REGISTERED_RIDERS = new HashMap<>();    //创建已注册骑士列表

    public static void registerRider(RiderConfig config){
        REGISTERED_RIDERS.put(config.getRiderId(), config);  //将新的RiderConfig整体纳入到列表中
    }

    //获取已注册骑士列表
    public static Collection<RiderConfig> getRegisteredRiders() {
        return REGISTERED_RIDERS.values();
    }

    @Nullable
    public static RiderConfig getRider(ResourceLocation id) {
        return REGISTERED_RIDERS.get(id);
    }

}