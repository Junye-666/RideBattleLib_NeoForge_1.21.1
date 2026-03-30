package com.jpigeon.ridebattlelib.server.system;

import com.jpigeon.ridebattlelib.common.config.RiderConfig;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import net.minecraft.world.entity.player.Player;

public class PenaltySystem {
    private PenaltySystem(){}
    private static final PenaltySystem PENALTY_SYSTEM = new PenaltySystem();

    public static PenaltySystem getInstance() {
        return PENALTY_SYSTEM;
    }

    public void startCooldown(Player player, long seconds) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        data.setPenaltyCooldownEnd(System.currentTimeMillis() + seconds * 1000L);
        player.addTag("penalty_cooldown");
    }

    public boolean isInCooldown(Player player) {
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return false;
        return config.getPenaltyStrategy().isInCooldown(player);
    }

    public void penaltyUnhenshin(Player player) {
        if (player.level().isClientSide()) return;

        // 强制解除变身
        HenshinSystem.getInstance().unHenshin(player);
        RiderConfig config = RiderConfig.findActiveDriverConfig(player);
        if (config == null) return;


        // 设生命值为安全值
        player.setHealth(config.getPenaltyStrategy().getResetHealth());
        config.getPenaltyStrategy().performPenaltyUnhenshin(player);
    }
}
