package com.creaddinscart.pixelchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

/**
 * New (1.21.6+) texture pipeline patcher. Each block sprite keeps its own backing {@link NativeImage}
 * inside {@code SpriteContents}; we overwrite that image with the user's pixels and then re-blit the
 * whole block atlas via {@code SpriteAtlasTexture.upload()} (both widened in pixelchanger.accesswidener).
 * Re-uploading only happens when the atlas no longer matches the edits, so it is effectively a no-op
 * in steady state and self-heals after a resource reload.
 */
public final class AtlasPatcher {

    private AtlasPatcher() {
    }

    public static void apply(MinecraftClient client) {
        SpriteAtlasTexture atlas;
        try {
            atlas = client.getAtlasManager().getAtlasTexture(Atlases.BLOCKS);
        } catch (Throwable notReady) {
            return;
        }
        if (atlas == null) {
            return;
        }

        Sprite missing;
        try {
            missing = atlas.getMissingSprite();
        } catch (Throwable notReady) {
            return;
        }

        boolean changed = false;
        for (String key : new ArrayList<>(PixelStore.editedTextureIds())) {
            Identifier id = Identifier.tryParse(key);
            if (id == null) {
                continue;
            }
            PixelStore.Edit edit = PixelStore.edit(key);
            if (edit == null) {
                continue;
            }

            Sprite sprite;
            try {
                sprite = atlas.getSprite(id);
            } catch (Throwable t) {
                continue;
            }
            if (sprite == null || sprite == missing) {
                continue;
            }

            NativeImage image = sprite.getContents().image; // widened
            if (image == null) {
                continue;
            }
            int w = Math.min(image.getWidth(), edit.width);
            int h = Math.min(image.getHeight(), edit.height);
            for (int y = 0; y < h; y++) {
                int row = y * edit.width;
                for (int x = 0; x < w; x++) {
                    int want = edit.pixels[row + x];
                    if (image.getColorArgb(x, y) != want) {
                        image.setColorArgb(x, y, want);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            atlas.upload(); // widened
        }
    }
}
