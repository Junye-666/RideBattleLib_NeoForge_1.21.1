package com.jpigeon.ridebattlelib.core;

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
    public static final KeyMapping HENSHIN_KEY = new KeyMapping(
            "key.ridebattlelib.henshin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.ridebattlelib"

    );
}
