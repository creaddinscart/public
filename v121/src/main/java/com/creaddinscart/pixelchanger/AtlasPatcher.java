package com.creaddinscart.pixelchanger;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * 1.21.0 (old OpenGL) texture pipeline patcher.
 *
 * The block atlas is a single OpenGL texture; {@code Sprite.upload()} writes a sprite's backing
 * {@link NativeImage} into whatever texture is currently bound, so we bind the block atlas first.
 * 1.21.0's {@code NativeImage.setColor/getColor} use little-endian RGBA (ABGR as an int), whereas our
 * stored pixels are ARGB, so channels are swapped on the way in/out.
 */
public final class AtlasPatcher {

    private AtlasPatcher() {
    }

    private static int argbToAbgr(int argb) {
        return (argb & 0xFF000000)
            | ((argb & 0x00FF0000) >> 16)
            | (argb & 0x0000FF00)
            | ((argb & 0x000000FF) << 16);
    }

    public static void apply(MinecraftClient client) {
        Function<Identifier, Sprite> blockAtlas;
        try {
            blockAtlas = client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        } catch (Throwable notReady) {
            return;
        }
        if (blockAtlas == null) {
            return;
        }

        boolean anyBound = false;

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
                sprite = blockAtlas.apply(id);
            } catch (Throwable t) {
                continue;
            }
            if (sprite == null || sprite.getContents() == null) {
                continue;
            }
            // Skip the "missing" sprite (its contents id is not the texture we asked for).
            if (!id.equals(sprite.getContents().getId())) {
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
                    int want = argbToAbgr(edit.pixels[row + x]);
                    if (image.getColor(x, y) != want) {
                        image.setColor(x, y, want);
                        changed = true;
                    }
                }
            }
            if (changed) {
                if (!anyBound) {
                    RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
                    anyBound = true;
                }
                sprite.upload();
            }
        }
    }
}
