package com.creaddinscart.pixelchanger;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Portable driver for applying pixel edits into the block atlas. The actual atlas patching is
 * version-specific and lives in {@code AtlasPatcher} (one per texture-pipeline variant). We run
 * it every client tick (render thread): {@code AtlasPatcher.apply} is cheap and only re-uploads
 * when the atlas no longer matches our edits, which also transparently restores edits after a
 * resource reload (F3+T, joining a world, ...) that re-bakes the atlas back to vanilla pixels.
 */
public final class TexturePatch {

    private TexturePatch() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(TexturePatch::tick);
    }

    private static void tick(MinecraftClient client) {
        if (PixelStore.editedTextureIds().isEmpty()) {
            return;
        }
        safeApply(client);
    }

    /** Called right after the user closes the editor so changes show up immediately. */
    public static void reapply() {
        safeApply(PixelChangerClient.client());
    }

    private static void safeApply(MinecraftClient client) {
        if (client == null) {
            return;
        }
        try {
            AtlasPatcher.apply(client);
        } catch (Throwable ignored) {
            // Atlas may not be initialized yet (e.g. still loading); retry next tick.
        }
    }
}
