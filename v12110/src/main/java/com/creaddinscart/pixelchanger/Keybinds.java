package com.creaddinscart.pixelchanger;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** 1.21.10 / 1.21.11: keybind category is a {@code KeyBinding.Category} record. */
public final class Keybinds {
    private Keybinds() {
    }

    public static KeyBinding editKey() {
        return new KeyBinding(
            "key.pixelchanger.edit",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            KeyBinding.Category.create(Identifier.of("pixelchanger", "main"))
        );
    }
}
