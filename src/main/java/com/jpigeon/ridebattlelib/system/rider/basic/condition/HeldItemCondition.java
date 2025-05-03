package com.jpigeon.ridebattlelib.system.rider.basic.condition;

import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class HeldItemCondition implements HenshinCondition {
    private final Item mainHand;
    private final Item offHand;

    public HeldItemCondition(Item mainHand, Item offHand) {
        this.mainHand = mainHand;
        this.offHand = offHand;
    }

    @Override
    public boolean check(Player player, RiderConfig config) {
        return player.getMainHandItem().is(mainHand) &&
                player.getOffhandItem().is(offHand);
    }
}
