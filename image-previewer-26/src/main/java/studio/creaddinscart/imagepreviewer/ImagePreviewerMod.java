package studio.creaddinscart.imagepreviewer;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.chat.ChatLinkDetector;
import studio.creaddinscart.imagepreviewer.config.ModConfig;
import studio.creaddinscart.imagepreviewer.net.ImageFetcher;
import studio.creaddinscart.imagepreviewer.render.ImageRenderer;

import java.nio.file.Path;

/**
 * Image Previewer — Fabric MOD entry point for Minecraft 1.21.4.
 * <p>
 * Built by CREADDINSCART STUDIO. Detects image URLs in chat and renders the linked
 * image as a HUD overlay when the player clicks the link.
 * </p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ImagePreviewerMod implements ClientModInitializer {

    public static final String MOD_ID = "imagepreviewer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ImageFetcher imageFetcher;
    public static ImageRenderer imageRenderer;
    public static ModConfig config;
    public static ChatLinkDetector linkDetector;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[ImagePreviewer] Initializing client subsystems...");

        Path configPath = MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config").resolve(MOD_ID + ".json");

        config = ModConfig.load(configPath);
        imageFetcher = new ImageFetcher(config.getMaxImageSizeMB());
        imageRenderer = new ImageRenderer(config);
        linkDetector = new ChatLinkDetector();

        // HUD overlay draw pass.
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (config.isEnabled() && imageRenderer != null) {
                imageRenderer.renderOverlay(context);
            }
        });

        registerCommands(configPath);
        LOGGER.info("[ImagePreviewer] Client initialization complete.");
    }

    private void registerCommands(Path configPath) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(
                        ClientCommandManager.literal(MOD_ID)
                                .then(ClientCommandManager.literal("reload")
                                        .executes(ctx -> {
                                            config = ModConfig.load(configPath);
                                            imageFetcher.setMaxSizeMB(config.getMaxImageSizeMB());
                                            ctx.getSource().sendFeedback(
                                                    Text.translatable("imagepreviewer.config.reloaded"));
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("dismiss")
                                        .executes(ctx -> {
                                            imageRenderer.dismiss();
                                            return 1;
                                        }))
                                .then(ClientCommandManager.literal("toggle")
                                        .executes(ctx -> {
                                            config.setEnabled(!config.isEnabled());
                                            config.save(configPath);
                                            ctx.getSource().sendFeedback(
                                                    Text.translatable("imagepreviewer.config.toggled",
                                                            config.isEnabled() ? "enabled" : "disabled"));
                                            return 1;
                                        }))
                )
        );
    }
}
