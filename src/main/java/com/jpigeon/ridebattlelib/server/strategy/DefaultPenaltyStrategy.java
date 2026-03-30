package com.jpigeon.ridebattlelib.server.strategy;

import com.jpigeon.ridebattlelib.Config;
import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.common.api.IPenaltyStrategy;
import com.jpigeon.ridebattlelib.common.data.RiderAttachments;
import com.jpigeon.ridebattlelib.common.data.RiderData;
import com.jpigeon.ridebattlelib.common.event.PenaltyEvent;
import com.jpigeon.ridebattlelib.common.util.HenshinUtils;
import com.jpigeon.ridebattlelib.server.system.PenaltySystem;
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
import net.neoforged.neoforge.common.NeoForge;

import java.util.Random;

public class DefaultPenaltyStrategy implements IPenaltyStrategy {
    @Override
    public boolean penaltyEnabled() { return Config.PENALTY_ENABLED.get(); }
    @Override
    public float getPenaltyThreshold() { return Config.PENALTY_THRESHOLD.get().floatValue(); }
    @Override
    public long getCooldownDuration() { return Config.COOLDOWN_DURATION.get().longValue(); }
    @Override
    public float getExplosionPower() { return Config.EXPLOSION_POWER.get().floatValue(); }
    @Override
    public float getKnockBackStrength() { return Config.KNOCKBACK_STRENGTH.get().floatValue(); }
    @Override
    public float getResetHealth() { return Config.PENALTY_RESET_HEALTH.get(); }

    @Override
    public boolean shouldTriggerPenalty(Player player) {
        if (!penaltyEnabled()) return false;
        return HenshinUtils.isTransformed(player) &&
                player.getHealth() <= getPenaltyThreshold() &&
                !isInCooldown(player);
    }

    @Override
    public void performPenaltyUnhenshin(Player player) {
        // 爆炸
        PenaltyEvent.Explosion explosion = new PenaltyEvent.Explosion(player);
        NeoForge.EVENT_BUS.post(explosion);
        if (!explosion.isCanceled()) {
            player.level().explode(player,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    getExplosionPower(),
                    false,
                    Level.ExplosionInteraction.NONE);
        }

        PenaltyEvent.Sound sound = new PenaltyEvent.Sound(player);
        NeoForge.EVENT_BUS.post(sound);
        if (!sound.isCanceled()) {
            // 播放爆炸音效
            if (!player.level().isClientSide()) {
                player.level().playSound(
                        player,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F + player.level().getRandom().nextFloat() * 0.2F
                );
            }
        }

        // 击飞
        Vec3 knockBack = player.getLookAngle().reverse().scale(getKnockBackStrength()).add(0, 1.0, 0);
        player.setDeltaMovement(knockBack);
        player.hurtMarked = true;

        // 保护效果
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));

        // 启动冷却
        PenaltySystem.getInstance().startCooldown(player, getCooldownDuration());

        PenaltyEvent.Particle particle = new PenaltyEvent.Particle(player);
        NeoForge.EVENT_BUS.post(particle);
        if (!particle.isCanceled()) {
            // 视觉特效
            for (int i = 0; i < 10; i++) {
                player.level().addParticle(ParticleTypes.LARGE_SMOKE,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        (player.getRandom().nextDouble() - 0.5) * 0.5,
                        0.1,
                        (player.getRandom().nextDouble() - 0.5) * 0.5);
            }
        }


        int cooldown = Math.toIntExact(getCooldownDuration());
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                cooldown * 20,
                0,
                false,
                true
        ));

        Random random = new Random();
        int chance = random.nextInt(100);
        if (chance < 10) {
            player.displayClientMessage(
                    Component.literal("我的身体已经菠萝菠萝哒!")
                            .withStyle(ChatFormatting.RED), true
            );
        }
        if (Config.DEBUG_MODE.get()) {
            RideBattleLib.LOGGER.debug("玩家 {} 触发吃瘪系统", player.getName().getString());
        }
    }

    @Override
    public boolean isInCooldown(Player player) {
        RiderData data = player.getData(RiderAttachments.RIDER_DATA);
        return data.isInPenaltyCooldown();
    }
}
