package studio.shitpub.videorenderer.render;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Version shim (Minecraft 1.21): KeyBinding used a plain String category here.
 * Built by CREADDINSCART STUDIO.
 */
public final class KeybindCompat {
    private KeybindCompat() {}

    public static KeyBinding createOpenKey() {
        return new KeyBinding(
                "key.videorenderer.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.videorenderer");
    }
}
