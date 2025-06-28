package com.jpigeon.ridebattlelib.core.system.penalty;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.api.IPenaltySystem;
import com.jpigeon.ridebattlelib.core.system.attachment.ModAttachments;
import com.jpigeon.ridebattlelib.core.system.attachment.PlayerPersistentData;
import com.jpigeon.ridebattlelib.core.system.henshin.HenshinSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class PenaltySystem implements IPenaltySystem {
    public static final PenaltySystem PENALTY_SYSTEM = new PenaltySystem();

    public static PenaltySystem getInstance() {
        return PenaltySystemHolder.INSTANCE;
    }

    private static class PenaltySystemHolder {
        private static final PenaltySystem INSTANCE = new PenaltySystem();
    }

    public float getPenaltyThreshold() {
        return Config.PENALTY_THRESHOLD.get().floatValue();
    }

    public int getCooldownDuration() {
        return Config.COOLDOWN_DURATION.get();
    }

    public float getExplosionPower() {
        return Config.EXPLOSION_POWER.get().floatValue();
    }

    public static boolean shouldTriggerPenalty(Player player) {
        PenaltySystem instance = getInstance();
        return HenshinSystem.INSTANCE.isTransformed(player) &&
                player.getHealth() <= instance.getPenaltyThreshold() &&
                !instance.isInCooldown(player);
    }

    @Override
    public void forceUnhenshin(Player player) {
        if (player.level().isClientSide()) return;

        // 1. 强制解除变身
        HenshinSystem.INSTANCE.unHenshin(player);

        // 2. 创建爆炸效果
        player.level().explode(player,
                player.getX(), player.getY() + 0.5, player.getZ(),
                getExplosionPower(),
                false,
                Level.ExplosionInteraction.NONE);

        // 3. 播放爆炸音效
        if (!player.level().isClientSide()) {
            player.level().playSound(
                    player,
                    player.getX(), player.getY(), player.getZ(), // 精确坐标版本
                    SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F + player.level().random.nextFloat() * 0.2F // 随机音高变化
            );
        }

        // 4. 击飞玩家
        Vec3 knockBack = player.getLookAngle().reverse().scale(1.5).add(0, 1.0, 0);
        player.setDeltaMovement(knockBack);
        player.hurtMarked = true;

        // 5. 添加保护效果
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));

        // 6. 启动冷却
        startCooldown(player, getCooldownDuration());

        // 7. 视觉特效
        for (int i = 0; i < 10; i++) {
            player.level().addParticle(ParticleTypes.LARGE_SMOKE,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    (player.getRandom().nextDouble() - 0.5) * 0.5,
                    0.1,
                    (player.getRandom().nextDouble() - 0.5) * 0.5);
        }

        int cooldown = getCooldownDuration();
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                cooldown * 20,  // 秒转tick
                0,
                false,
                true
        ));

        Random random = new Random();
        int chance =  random.nextInt(100);
        if (chance < 10){
            player.displayClientMessage(
                    Component.literal("我的身体已经菠萝菠萝哒!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        } else if (chance < 30) {
            player.displayClientMessage(
                    Component.literal("不能再打下去了!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }

        RideBattleLib.LOGGER.info("玩家 {} 触发吃瘪系统", player.getName().getString());
    }

    @Override
    public void startCooldown(Player player, int seconds) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        data.setPenaltyCooldownEnd(System.currentTimeMillis() + seconds * 1000L);
        player.addTag("penalty_cooldown");
    }


    @Override
    public boolean isInCooldown(Player player) {
        PlayerPersistentData data = player.getData(ModAttachments.PLAYER_DATA);
        return data.isInPenaltyCooldown();
    }
}
