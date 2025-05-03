package com.jpigeon.ridebattlelib.system.rider.basic.condition;

import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class BeltSlotCondition implements HenshinCondition {
    /**
     * TODO: 基本物品与进阶物品等级划分
     * @param beltSlottedItem 腰带插入物品
     */

    public BeltSlotCondition(Item beltSlottedItem) {

    }

    @Override
    public boolean check(Player player, RiderConfig config) {
        return false;
    }
}
