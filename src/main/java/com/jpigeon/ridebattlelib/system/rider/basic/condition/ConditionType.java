package com.jpigeon.ridebattlelib.system.rider.basic.condition;

import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * 这个系统实现假面骑士变身所需条件检测+创建
 */

public enum ConditionType {
    HELD_ITEM,      //手持物品
    BELT_SLOT,    //腰带插入
    PLAYER_STATE,   //玩家状态
    CUSTOM_LOGIC;   //扩展逻辑

    //根据类型和参数创建条件
    public HenshinCondition createCondition(Map<String, Object> params) {
        return switch (this) {
            case HELD_ITEM -> new HeldItemCondition(
                    (Item) params.get("mainHand"),
                    (Item) params.get("offHand")
            );
            case BELT_SLOT -> new BeltSlotCondition(
                    (Item) params.get("beltItem")
            );
            case PLAYER_STATE -> new PlayerStateCondition(
                    (Float) params.get("minHealth"),
                    (Integer) params.get("minLevel")
            );
            case CUSTOM_LOGIC -> ((player, config) -> (Boolean) params.get("customCheck"));
            default -> throw new IllegalArgumentException("未知的条件类型" + this);
        };
    }
}
