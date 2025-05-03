package com.jpigeon.ridebattlelib.system.rider.basic.condition;

import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;
import net.minecraft.world.entity.player.Player;

public class PlayerStateCondition implements HenshinCondition {
    public PlayerStateCondition(Float minHealth, Integer minLevel) {
    }

    @Override
    public boolean check(Player player, RiderConfig config) {
        return false;
    }
}
