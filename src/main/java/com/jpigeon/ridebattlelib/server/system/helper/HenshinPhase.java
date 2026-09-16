package com.jpigeon.ridebattlelib.server.system.helper;

public enum HenshinPhase {
    IDLE,
    ACTIVATING,        // driverAction 已触发，正在匹配形态
    PAUSED,            // 等待 completeHenshin
    TRANSFORMING,      // 有 autoTicks 或 shouldPause 的中间态
    TRANSFORMED,
    SWITCHING,
    UNHENSHIN
}
