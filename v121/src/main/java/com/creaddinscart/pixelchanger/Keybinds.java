package com.creaddinscart.pixelchanger;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** 1.21.0: keybind category is a plain String. */
public final class Keybinds {
    private Keybinds() {
    }

    public static KeyBinding editKey() {
        return new KeyBinding(
            "key.pixelchanger.edit",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "category.pixelchanger"
        );
    }
}
