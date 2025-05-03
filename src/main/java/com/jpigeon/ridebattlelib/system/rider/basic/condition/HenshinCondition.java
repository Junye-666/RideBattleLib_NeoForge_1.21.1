package com.jpigeon.ridebattlelib.system.rider.basic.condition;

import com.jpigeon.ridebattlelib.system.rider.basic.RiderConfig;
import net.minecraft.world.entity.player.Player;

public interface HenshinCondition {
    boolean check(Player player, RiderConfig config);
}
