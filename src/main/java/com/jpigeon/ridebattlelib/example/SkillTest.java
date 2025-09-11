package com.jpigeon.ridebattlelib.example;

import com.jpigeon.ridebattlelib.RideBattleLib;
import com.jpigeon.ridebattlelib.core.system.skill.SkillSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class SkillTest {
    public static final ResourceLocation ALPHA_KICK = ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_kick");
    public static final ResourceLocation ALPHA_PUNCH = ResourceLocation.fromNamespaceAndPath(RideBattleLib.MODID, "alpha_punch");

    public static void registerSkill(){
        SkillSystem.registerSkillName(ALPHA_KICK, Component.translatable("skill.alpha_kick"));
        SkillSystem.registerSkillName(ALPHA_PUNCH, Component.translatable("skill.alpha_punch"));

        ExampleBasic.alphaBaseForm.addSkill(ALPHA_KICK);
        ExampleBasic.alphaBaseForm.addSkill(ALPHA_PUNCH);
    }

    public static void init() {
        registerSkill();
    }
}
