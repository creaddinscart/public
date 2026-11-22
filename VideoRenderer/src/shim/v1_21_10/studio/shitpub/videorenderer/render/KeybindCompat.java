package studio.shitpub.videorenderer.render;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Version shim (Minecraft 1.21.11): KeyBinding takes a KeyBinding.Category record.
 * Built by CREADDINSCART STUDIO.
 */
public final class KeybindCompat {
    private KeybindCompat() {}

    public static KeyBinding createOpenKey() {
        return new KeyBinding(
                "key.videorenderer.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                KeyBinding.Category.create(Identifier.of("videorenderer", "category")));
    }
}
