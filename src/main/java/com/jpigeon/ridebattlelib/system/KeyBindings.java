package com.jpigeon.ridebattlelib.system;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping UNHENSHIN_KEY = new KeyMapping(
            "key.ridebattlelib.unhenshin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.ridebattlelib"
    );

}
