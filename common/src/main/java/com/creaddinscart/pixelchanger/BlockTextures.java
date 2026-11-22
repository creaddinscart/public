package com.creaddinscart.pixelchanger;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

/**
 * Resolves which texture (sprite id, e.g. {@code minecraft:block/stone}) a targeted block
 * uses, so the editor can load and override it. Uses the block registry name plus the common
 * multi-texture suffixes and keeps the first id that actually has a PNG on disk.
 */
public final class BlockTextures {

    private BlockTextures() {
    }

    /** Returns a sprite id like {@code minecraft:block/stone}, or null if none could be found. */
    public static Identifier resolve(BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        ResourceManager rm = client.getResourceManager();
        Identifier blockId = Registries.BLOCK.getId(state.getBlock());
        String ns = blockId.getNamespace();
        String path = blockId.getPath();

        String[] candidates = {
            path,
            path + "_side",
            path + "_top",
            path + "_end",
            path + "_front",
            path + "_normal",
        };
        for (String c : candidates) {
            Identifier sprite = Identifier.tryParse(ns + ":block/" + c);
            if (sprite != null && textureResourceExists(rm, sprite)) {
                return sprite;
            }
        }
        // Fall back to the plain id even if we could not confirm the PNG (editor will report cleanly).
        return Identifier.tryParse(ns + ":block/" + path);
    }

    private static boolean textureResourceExists(ResourceManager rm, Identifier spriteId) {
        Identifier png = Identifier.tryParse(spriteId.getNamespace() + ":textures/" + spriteId.getPath() + ".png");
        return png != null && rm.getResource(png).isPresent();
    }
}
