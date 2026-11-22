package studio.creaddinscart.imagepreviewer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.chat.ChatLinkDetector;
import studio.creaddinscart.imagepreviewer.config.ModConfig;
import studio.creaddinscart.imagepreviewer.net.ImageFetcher;
import studio.creaddinscart.imagepreviewer.render.ImageRenderer;

import java.nio.file.Path;

/**
 * Image Previewer — Main Fabric MOD entry point.
 * <p>
 * Provides in-game preview of image URLs shared in chat. When a player clicks
 * a detected image link, the MOD fetches and renders the image as a HUD overlay.
 * </p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ImagePreviewerMod implements ClientModInitializer {

    /** The unique mod identifier used for resource registration and config paths. */
    public static final String MOD_ID = "imagepreviewer";

    /** Logger instance scoped to this mod. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ─── Static singleton references (initialized during client startup) ───────

    /** Handles async downloading and caching of remote images. */
    public static ImageFetcher imageFetcher;

    /** Manages OpenGL texture upload and HUD overlay rendering. */
    public static ImageRenderer imageRenderer;

    /** User-configurable settings loaded from JSON. */
    public static ModConfig config;

    /** Regex-based detector for image URLs in chat text. */
    public static ChatLinkDetector linkDetector;

    /**
     * Called by Fabric Loader when the client environment is ready.
     * <p>
     * Initialization order:
     * <ol>
     *   <li>Load configuration from disk</li>
     *   <li>Instantiate subsystems (fetcher, renderer, detector)</li>
     *   <li>Register HUD render callback for image overlay</li>
     *   <li>Register client commands for config management</li>
     * </ol>
     */
    @Override
    public void onInitializeClient() {
        LOGGER.info("[ImagePreviewer] Initializing client subsystems...");

        // Resolve config path relative to the game instance root
        Path configPath = MinecraftClient.getInstance().runDirectory
                .toPath()
                .resolve("config")
                .resolve(MOD_ID + ".json");

        // Load user configuration (creates default file if absent)
        config = ModConfig.load(configPath);
        LOGGER.info("[ImagePreviewer] Config loaded: enabled={}, maxImageSize={}MB",
                config.isEnabled(), config.getMaxImageSizeMB());

        // Initialize the async image fetcher with config-driven size limit
        imageFetcher = new ImageFetcher(config.getMaxImageSizeMB());

        // Initialize the OpenGL-based HUD renderer
        imageRenderer = new ImageRenderer(config);

        // Initialize the chat link detector with known image hosting patterns
        linkDetector = new ChatLinkDetector();

        // Register HUD overlay render — draws the active image preview each frame
        HudElementRegistry.addLast(
                Identifier.of(MOD_ID, "image_overlay"),
                this::renderImageOverlay
        );

        // Register client commands for runtime config adjustment
        registerCommands();

        LOGGER.info("[ImagePreviewer] Client initialization complete.");
    }

    /**
     * HUD render callback invoked every frame by Fabric's HUD rendering pipeline.
     * <p>
     * Delegates to {@link ImageRenderer#renderOverlay(DrawContext, float)} which
     * handles the actual OpenGL draw calls. The renderer is a no-op when no image
     * is currently active.
     * </p>
     *
     * @param context   the current draw context for HUD rendering
     * @param tickDelta partial tick for smooth animation interpolation
     */
    private void renderImageOverlay(DrawContext context, float tickDelta) {
        if (config.isEnabled() && imageRenderer != null) {
            imageRenderer.renderOverlay(context, tickDelta);
        }
    }

    /**
     * Registers client-side commands for managing the mod at runtime.
     * Commands:
     * <ul>
     *   <li>{@code /imagepreviewer reload} — reload config from disk</li>
     *   <li>{@code /imagepreviewer dismiss} — close the current image preview</li>
     *   <li>{@code /imagepreviewer toggle} — enable/disable the mod</li>
     * </ul>
     */
    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    net.minecraft.client.command.CommandManager.literal(MOD_ID)
                            .then(net.minecraft.client.command.CommandManager.literal("reload")
                                    .executes(ctx -> {
                                        Path configPath = MinecraftClient.getInstance().runDirectory
                                                .toPath().resolve("config").resolve(MOD_ID + ".json");
                                        config = ModConfig.load(configPath);
                                        imageFetcher.setMaxSizeMB(config.getMaxImageSizeMB());
                                        ctx.getSource().sendFeedback(
                                                Text.translatable("imagepreviewer.config.reloaded"));
                                        return 1;
                                    }))
                            .then(net.minecraft.client.command.CommandManager.literal("dismiss")
                                    .executes(ctx -> {
                                        imageRenderer.dismiss();
                                        return 1;
                                    }))
                            .then(net.minecraft.client.command.CommandManager.literal("toggle")
                                    .executes(ctx -> {
                                        config.setEnabled(!config.isEnabled());
                                        config.save(MinecraftClient.getInstance().runDirectory
                                                .toPath().resolve("config").resolve(MOD_ID + ".json"));
                                        ctx.getSource().sendFeedback(
                                                Text.translatable("imagepreviewer.config.toggled",
                                                        config.isEnabled() ? "enabled" : "disabled"));
                                        return 1;
                                    }))
            );
        });
    }
}
