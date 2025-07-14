package com.jpigeon.ridebattlelib.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping UNHENSHIN_KEY = new KeyMapping(
            "key.ridebattlelib.unhenshin",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.ridebattlelib"
    );
    public static final KeyMapping DRIVER_KEY = new KeyMapping(
            "key.ridebattlelib.driver_activate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.ridebattlelib"
    );
    public static final KeyMapping RETURN_ITEMS_KEY = new KeyMapping(
            "key.ridebattlelib.return_items",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "category.ridebattlelib"
    );
    public static final KeyMapping SKILL_KEY = new KeyMapping(
            "key.ridebattlelib.skill",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.ridebattlelib"
    );
}
