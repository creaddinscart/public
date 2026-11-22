package studio.creaddinscart.imagepreviewer.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.ImagePreviewerMod;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Handles the "click to preview" interaction when a player clicks an image URL in chat.
 * <p>
 * This class encapsulates the logic triggered by a {@link ClickEvent} attached to
 * detected image URLs. It coordinates between the {@code ImageFetcher} (async download)
 * and {@code ImageRenderer} (GL upload + display), handling all error cases gracefully
 * with user-facing feedback messages.
 * </p>
 *
 * <p>Design notes:
 * <ul>
 *   <li>The fetcher callback runs on a background thread; the renderer call is
 *       marshalled back to the render thread via {@code client.execute()}.</li>
 *   <li>Rapid clicks on the same URL are deduplicated by the fetcher's in-flight tracking.</li>
 *   <li>Error messages use translation keys for localization support.</li>
 * </ul>
 * </p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ClickToRenderAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreviewerMod.MOD_ID + "/ClickAction");

    /**
     * Creates a {@link ClickEvent} that triggers image preview when activated.
     * <p>
     * Uses a custom run-command event with an internal command prefix that the
     * mod intercepts. This approach avoids conflicts with vanilla click behaviors
     * (suggest_command, open_url) while remaining compatible with Fabric's
     * chat rendering pipeline.
     * </p>
     *
     * @param imageUrl the URL of the image to preview on click
     * @return a configured ClickEvent for attachment to chat text components
     */
    public static ClickEvent createPreviewClick(String imageUrl) {
        // Use run_command with a custom internal command that only this mod processes.
        // The leading slash and mod-namespace prevent collision with player-typed commands.
        String command = "/imagepreviewer_preview " + imageUrl;
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
    }

    /**
     * Creates a {@link HoverEvent} that displays "click to preview" tooltip text
     * when the player hovers over a detected image URL.
     *
     * @param imageUrl the URL being hovered (shown in tooltip for context)
     * @return a hover event with localized tooltip text
     */
    public static HoverEvent<?> createPreviewHover(String imageUrl) {
        MutableText tooltip = Text.empty()
                .append(Text.translatable("imagepreviewer.tooltip.click_to_preview")
                        .formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal("\n"))
                .append(Text.literal(truncateUrl(imageUrl, 60))
                        .formatted(Formatting.GRAY));

        return new HoverEvent<>(HoverEvent.Action.SHOW_TEXT, tooltip);
    }

    /**
     * Executes the image preview flow: fetch → decode → render.
     * <p>
     * This is the main entry point called when the internal preview command is
     * detected. It performs the full pipeline:
     * <ol>
     *   <li>Check if the mod is enabled</li>
     *   <li>Check the fetcher cache for an instant hit</li>
     *   <li>Initiate async download if not cached</li>
     *   <li>On success: marshal to render thread and upload texture</li>
     *   <li>On failure: send user-facing error message in chat</li>
     * </ol>
     * </p>
     *
     * @param imageUrl the image URL to fetch and display
     */
    public static void execute(String imageUrl) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null) {
            LOGGER.error("MinecraftClient is null — cannot execute preview.");
            return;
        }

        // Guard: mod disabled
        if (!ImagePreviewerMod.config.isEnabled()) {
            sendChatMessage(client, Text.translatable("imagepreviewer.error.disabled")
                    .formatted(Formatting.YELLOW));
            return;
        }

        // Guard: invalid URL
        if (imageUrl == null || imageUrl.isBlank()) {
            sendChatMessage(client, Text.translatable("imagepreviewer.error.invalid_url")
                    .formatted(Formatting.RED));
            return;
        }

        LOGGER.info("Preview requested for: {}", imageUrl);

        // Check cache first — provides instant display for previously viewed images
        BufferedImage cached = ImagePreviewerMod.imageFetcher.getCached(imageUrl);
        if (cached != null) {
            LOGGER.debug("Cache hit for: {}", imageUrl);
            client.execute(() -> ImagePreviewerMod.imageRenderer.displayImage(cached));
            return;
        }

        // Show loading indicator in chat
        sendChatMessage(client, Text.translatable("imagepreviewer.status.loading")
                .formatted(Formatting.AQUA, Formatting.ITALIC));

        // Async fetch with error-handling callback
        Consumer<BufferedImage> callback = image -> {
            if (image != null) {
                // Success: marshal texture upload to the render thread
                client.execute(() -> {
                    ImagePreviewerMod.imageRenderer.displayImage(image);
                    LOGGER.debug("Image preview displayed: {} ({}x{})",
                            imageUrl, image.getWidth(), image.getHeight());
                });
            } else {
                // Failure: determine the likely cause and send appropriate error
                client.execute(() -> sendFetchError(client, imageUrl));
            }
        };

        ImagePreviewerMod.imageFetcher.fetchAsync(imageUrl, callback);
    }

    /**
     * Sends an appropriate error message based on the URL characteristics.
     * Distinguishes between likely network issues, size violations, and format errors.
     *
     * @param client   the Minecraft client instance
     * @param imageUrl the URL that failed to load
     */
    private static void sendFetchError(MinecraftClient client, String imageUrl) {
        Text errorText;

        // Heuristic: check if the extension suggests an unsupported format
        if (imageUrl != null && imageUrl.matches("(?i).*\\.(svg|tiff?|avif)$.*")) {
            // SVG/TIFF/AVIF may fail if ImageIO lacks a plugin for them
            errorText = Text.translatable("imagepreviewer.error.unsupported")
                    .formatted(Formatting.RED);
        } else {
            // Default: assume network or size failure
            errorText = Text.translatable("imagepreviewer.error.network")
                    .formatted(Formatting.RED);
        }

        sendChatMessage(client, errorText);
        LOGGER.warn("Image preview failed for: {}", imageUrl);
    }

    /**
     * Appends a system message to the player's chat HUD.
     * Safe to call from any thread via client.execute() wrapping at call sites.
     *
     * @param client the Minecraft client
     * @param message the text component to display
     */
    private static void sendChatMessage(MinecraftClient client, Text message) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(message);
        }
    }

    /**
     * Truncates a URL for display in tooltips, appending "..." if shortened.
     *
     * @param url      the full URL
     * @param maxChars maximum character length
     * @return truncated URL suitable for display
     */
    private static String truncateUrl(String url, int maxChars) {
        if (url == null) return "";
        if (url.length() <= maxChars) return url;
        return url.substring(0, maxChars - 3) + "...";
    }

    /**
     * Builds a styled MutableText component for an image URL that includes
     * the click and hover events for preview interaction.
     * <p>
     * The returned text can replace the plain URL in chat message components
     * to make it interactive.
     * </p>
     *
     * @param url the image URL to wrap
     * @return a styled text component with click-to-preview behavior
     */
    public static MutableText buildClickableText(String url) {
        return Text.literal(url)
                .styled(style -> style
                        .withColor(Formatting.AQUA)
                        .withUnderline(true)
                        .withClickEvent(createPreviewClick(url))
                        .withHoverEvent(createPreviewHover(url))
                );
    }
}
