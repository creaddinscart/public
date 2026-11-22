package com.creaddinscart.pixelchanger;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

import com.mojang.blaze3d.textures.GpuTexture;

import java.util.ArrayList;

/**
 * 1.21.10 texture pipeline patcher. Each block sprite can re-upload its own backing
 * {@link NativeImage} straight into the atlas GPU texture at its stitched position via the public
 * {@code Sprite.upload(GpuTexture)}. We overwrite the sprite's contents image (widened field) with
 * the user's pixels and re-upload only the sprites that no longer match.
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
        GpuTexture texture = atlas.getGlTexture();
        if (texture == null) {
            return;
        }
        Sprite missing;
        try {
            missing = atlas.getMissingSprite();
        } catch (Throwable notReady) {
            return;
        }

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
            boolean changed = false;
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
            if (changed) {
                sprite.upload(texture);
            }
        }
    }
}
