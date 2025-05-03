package com.jpigeon.ridebattlelib.system.rider.basic;

import java.util.ArrayList;
import java.util.List;

/**
 * 理解为管理所有被注册骑士的列表
 */

public class RiderRegistry {
    public static final List<RiderConfig> REGISTERED_RIDERS = new ArrayList<>();    //创建已注册骑士列表

    public static void registerRider(RiderConfig config){
        REGISTERED_RIDERS.add(config);  //将新的RiderConfig整体纳入到列表中
    }

    //获取已注册骑士列表
    public static List<RiderConfig> getRegisteredRiders(){
        return REGISTERED_RIDERS;
    }
}