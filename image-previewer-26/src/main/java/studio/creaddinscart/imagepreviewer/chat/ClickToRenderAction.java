package studio.creaddinscart.imagepreviewer.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.ImagePreviewerMod;

import java.awt.image.BufferedImage;

/**
 * Handles the "click to preview" interaction pipeline for an image URL in chat.
 *
 * @author GMA
 * @since 1.0.0
 */
public class ClickToRenderAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreviewerMod.MOD_ID + "/ClickAction");

    /**
     * Creates a {@link ClickEvent} that triggers image preview when activated.
     */
    public static ClickEvent createPreviewClick(String imageUrl) {
        String command = "/imagepreviewer_preview " + imageUrl;
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
    }

    /**
     * Creates a {@link HoverEvent} showing a "click to preview" tooltip.
     * On 1.21.4 {@link HoverEvent} is non-generic; {@code SHOW_TEXT} carries a {@link Text}.
     */
    public static HoverEvent createPreviewHover(String imageUrl) {
        MutableText tooltip = Text.empty()
                .append(Text.translatable("imagepreviewer.tooltip.click_to_preview")
                        .formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal("\n"))
                .append(Text.literal(truncateUrl(imageUrl, 60)).formatted(Formatting.GRAY));
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip);
    }

    /**
     * Executes the image preview flow: cache check -> async fetch -> render.
     */
    public static void execute(String imageUrl) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            LOGGER.error("MinecraftClient is null — cannot execute preview.");
            return;
        }

        if (!ImagePreviewerMod.config.isEnabled()) {
            sendChatMessage(client, Text.translatable("imagepreviewer.error.disabled").formatted(Formatting.YELLOW));
            return;
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            sendChatMessage(client, Text.translatable("imagepreviewer.error.invalid_url").formatted(Formatting.RED));
            return;
        }

        LOGGER.info("Preview requested for: {}", imageUrl);

        BufferedImage cached = ImagePreviewerMod.imageFetcher.getCached(imageUrl);
        if (cached != null) {
            client.execute(() -> ImagePreviewerMod.imageRenderer.displayImage(cached));
            return;
        }

        sendChatMessage(client, Text.translatable("imagepreviewer.status.loading").formatted(Formatting.AQUA));

        ImagePreviewerMod.imageFetcher.fetchAsync(imageUrl, image -> {
            if (image != null) {
                client.execute(() -> ImagePreviewerMod.imageRenderer.displayImage(image));
            } else {
                client.execute(() -> sendChatMessage(client,
                        Text.translatable("imagepreviewer.error.network").formatted(Formatting.RED)));
            }
        });
    }

    /**
     * Builds a styled, clickable chat component for an image URL.
     */
    public static MutableText buildClickableText(String url) {
        return Text.literal(url).styled(style -> style
                .withColor(Formatting.AQUA)
                .withUnderline(true)
                .withClickEvent(createPreviewClick(url))
                .withHoverEvent(createPreviewHover(url)));
    }

    private static void sendChatMessage(MinecraftClient client, Text message) {
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(message);
        }
    }

    private static String truncateUrl(String url, int maxChars) {
        if (url == null) return "";
        if (url.length() <= maxChars) return url;
        return url.substring(0, maxChars - 3) + "...";
    }
}
