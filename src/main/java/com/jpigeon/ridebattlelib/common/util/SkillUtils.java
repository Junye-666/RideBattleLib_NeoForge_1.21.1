package com.jpigeon.ridebattlelib.common.util;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 轻量级技能爆炸：AABB 检索 + 玩家归属伤害 + 单次粒子/音效。
 * <p>
 * 相比 {@code Level#explode}，本实现：
 * <ul>
 *   <li>不做射线投射（无 {@code getSeenPercent}）</li>
 *   <li>不做方块破坏检查</li>
 *   <li>只发一组粒子数据包</li>
 *   <li>伤害归属为 {@code Player}，兼容击杀统计/成就</li>
 * </ul>
 */
public class SkillUtils {
    private SkillUtils() {}

    /**
     * @param owner        伤害归属玩家（可为 null，则用 generic 源）
     * @param level        世界（服务端调用）
     * @param x/y/z        爆炸中心
     * @param radius       伤害半径
     * @param damage       中心伤害（边缘按距离线性衰减到 0）
     * @param knockback    是否击飞
     */
    public static void explode(
            @Nullable Player owner,
            Level level,
            double x, double y, double z,
            float radius,
            float damage,
            boolean knockback) {

        if (level.isClientSide()) return;

        ServerLevel server = (ServerLevel) level;

        // 1) 单次粒子爆发（一次数据包，非每方块）
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                x, y, z, 1, 0, 0, 0, 0);
        server.sendParticles(ParticleTypes.EXPLOSION,
                x, y, z, 4, radius * 0.3, radius * 0.3, radius * 0.3, 0.0);

        // 2) 单次音效
        level.playSound(null, x, y, z,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                1.0f, 1.0f);

        // 3) AABB 实体检索（无射线）
        AABB box = new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);

        DamageSource source = (owner != null)
                ? owner.damageSources().playerAttack(owner)
                : level.damageSources().generic();

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e.isAlive()
                        && e != owner
                        && !(e instanceof ArmorStand));

        for (LivingEntity t : targets) {
            double dist = Math.sqrt(t.distanceToSqr(x, y, z));
            if (dist > radius) continue;

            float falloff = (float) (1.0 - dist / radius);
            t.hurt(source, damage * falloff);

            if (knockback) {
                Vec3 dir = new Vec3(t.getX() - x, 0, t.getZ() - z).normalize();
                double power = 0.6 * falloff;
                t.push(dir.x * power, 0.4 * falloff, dir.z * power);
                t.hurtMarked = true;
            }
        }
    }

    /** 便捷重载：默认带击飞 */
    public static void explode(@Nullable Player owner, Level level,
                               double x, double y, double z,
                               float radius, float damage) {
        explode(owner, level, x, y, z, radius, damage, true);
    }
}
