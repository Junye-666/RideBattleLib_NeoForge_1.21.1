package com.jpigeon.ridebattlelib;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


@EventBusSubscriber(modid = RideBattleLib.MODID)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue PENALTY_ENABLED;
    public static final ModConfigSpec.IntValue PENALTY_THRESHOLD;
    public static final ModConfigSpec.IntValue COOLDOWN_DURATION;
    public static final ModConfigSpec.IntValue EXPLOSION_POWER;
    public static final ModConfigSpec.IntValue KNOCKBACK_STRENGTH;
    public static final ModConfigSpec.IntValue KEY_COOLDOWN_MS;
    public static final ModConfigSpec.IntValue INTERACTION_COOLDOWN_MS;
    public static final ModConfigSpec.BooleanValue DEBUG_MODE;

    static {
        PENALTY_ENABLED = BUILDER
                .define("penaltyEnabled", true);

        // 惩罚触发阈值（默认3次）
        PENALTY_THRESHOLD = BUILDER
                .comment("触发吃瘪生命阈值")
                .defineInRange("penaltyThreshold", 3, 1, 10);

        // 冷却时间（秒，默认30秒）
        COOLDOWN_DURATION = BUILDER
                .comment("吃瘪冷却(秒)")
                .defineInRange("cooldownDuration", 60, 0, 300);

        // 爆炸威力（默认2.0）
        EXPLOSION_POWER = BUILDER
                .comment("吃瘪触发时爆炸强度 (为0时取消)")
                .defineInRange("explosionPower", 2, 0, 10);

        KNOCKBACK_STRENGTH = BUILDER
                .comment("吃瘪触发时击退强度")
                .defineInRange("knockbackStrength", 2, 0, 20);

        KEY_COOLDOWN_MS = BUILDER
                .comment("按键防抖动毫秒")
                .defineInRange("keyCooldown", 150, 0, 400);

        INTERACTION_COOLDOWN_MS = BUILDER
                .comment("右键防抖动毫秒")
                .defineInRange("interactionCooldown", 250, 0, 400);

        DEBUG_MODE = BUILDER
                .define("logLevel", false);

        BUILDER.build();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (Config.DEBUG_MODE.get()){
            RideBattleLib.LOGGER.debug(
                    "Loaded config: penaltyEnabled={}, penaltyThreshold={}, cooldown={}s, explosionPower={}, knockbackStrength={}, keyCooldown={}, interactionCooldown={}, logLevel={}",
                    PENALTY_ENABLED.get(),
                    PENALTY_THRESHOLD.get(),
                    COOLDOWN_DURATION.get(),
                    EXPLOSION_POWER.get(),
                    KNOCKBACK_STRENGTH.get(),
                    KNOCKBACK_STRENGTH.get(),
                    INTERACTION_COOLDOWN_MS.get(),
                    DEBUG_MODE.get());
        }
    }
}
