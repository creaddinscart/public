package com.creaddinscart.pixelchanger;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public final class PixelChangerClient implements ClientModInitializer {

    private static KeyBinding editKey;
    private static MinecraftClient client;

    @Override
    public void onInitializeClient() {
        client = MinecraftClient.getInstance();
        PixelStore.init(FabricLoader.getInstance().getGameDir());

        editKey = KeyBindingHelper.registerKeyBinding(Keybinds.editKey());

        // Register the runtime texture override hook (patches the block atlas with edited pixels).
        TexturePatch.register();

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            while (editKey.wasPressed()) {
                openEditor(c);
            }
        });

        PixelChanger.LOGGER.info("{} ready - aim at a block and press F8. Built by Maker SX / CREADDINSCART STUDIO.",
            PixelChanger.MOD_NAME);
    }

    private static void openEditor(MinecraftClient c) {
        if (c.world == null || c.player == null) {
            return;
        }
        HitResult hit = c.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            c.player.sendMessage(Text.literal("[Pixel Changer] Look at a block first."), true);
            return;
        }
        BlockState state = c.world.getBlockState(((BlockHitResult) hit).getBlockPos());
        Identifier sprite = BlockTextures.resolve(state);
        if (sprite == null) {
            c.player.sendMessage(Text.literal("[Pixel Changer] No editable texture for that block."), true);
            return;
        }
        PixelStore.Edit edit = PixelStore.getOrLoad(sprite, c.getResourceManager());
        if (edit == null) {
            c.player.sendMessage(Text.literal("[Pixel Changer] Could not read texture: " + sprite), true);
            return;
        }
        c.setScreen(new PixelEditorScreen(sprite, edit));
    }

    /** Called by the editor when changes should be written to disk and pushed into the atlas. */
    public static void applyChanges() {
        PixelStore.save();
        TexturePatch.reapply();
    }

    public static MinecraftClient client() {
        return client != null ? client : MinecraftClient.getInstance();
    }
}
