package studio.creaddinscart.imagepreviewer.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.ImagePreviewerMod;
import studio.creaddinscart.imagepreviewer.config.ModConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Renders a fetched image as a centered HUD overlay on Minecraft 1.21.4.
 * <p>
 * Uses the modern texture pipeline: the {@link BufferedImage} is PNG-encoded,
 * decoded into a {@link NativeImage}, wrapped in a {@link NativeImageBackedTexture},
 * registered with the game's {@link net.minecraft.client.texture.TextureManager},
 * and finally blitted through {@link DrawContext#drawTexture}. No raw OpenGL calls.
 * </p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ImageRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreviewerMod.MOD_ID + "/ImageRenderer");

    /** Stable identifier under which the preview texture is registered. */
    private static final Identifier TEXTURE_ID =
            Identifier.of(ImagePreviewerMod.MOD_ID, "chat_preview");

    private final ModConfig config;

    private volatile boolean active = false;
    private int textureWidth = 0;
    private int textureHeight = 0;

    /** The currently registered preview texture, kept so we can close it on replace/dismiss. */
    private net.minecraft.client.texture.AbstractTexture currentTexture;

    public ImageRenderer(ModConfig config) {
        this.config = config;
    }

    /**
     * Uploads a {@link BufferedImage} as a registered game texture and activates the overlay.
     * <b>Must be called on the render thread</b> (call sites wrap with {@code client.execute}).
     *
     * @param img the decoded image to display
     */
    public void displayImage(BufferedImage img) {
        if (img == null) {
            LOGGER.warn("displayImage called with null image");
            return;
        }

        try {
            // Encode to PNG bytes so NativeImage can decode in the game's expected format.
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(img, "png", png);
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(png.toByteArray()));

            // Replace any previously registered preview texture.
            MinecraftClient client = MinecraftClient.getInstance();
            if (currentTexture != null) {
                currentTexture.close();
            }
            NativeImageBackedTexture tex = new NativeImageBackedTexture(nativeImage);
            client.getTextureManager().registerTexture(TEXTURE_ID, tex);
            this.currentTexture = tex;

            this.textureWidth = nativeImage.getWidth();
            this.textureHeight = nativeImage.getHeight();
            this.active = true;

            LOGGER.debug("Preview texture registered: {}x{}", textureWidth, textureHeight);
        } catch (Exception e) {
            LOGGER.error("Failed to register preview texture: {}", e.getMessage());
            this.active = false;
        }
    }

    /**
     * Draws the active image overlay as part of the HUD pass.
     *
     * @param context the current draw context
     */
    public void renderOverlay(DrawContext context) {
        if (!active || textureWidth == 0) {
            return;
        }

        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        // Fit within configured max dimensions, preserving aspect ratio; never upscale.
        float scale = Math.min((float) config.getMaxWidth() / textureWidth,
                (float) config.getMaxHeight() / textureHeight);
        scale = Math.min(scale, 1.0f);
        int drawW = Math.max(1, (int) (textureWidth * scale));
        int drawH = Math.max(1, (int) (textureHeight * scale));
        int x = (sw - drawW) / 2;
        int y = (sh - drawH) / 2;

        // Semi-transparent backdrop for contrast.
        int bgAlpha = (int) (config.getBackgroundOpacity() * 255);
        int bgColor = (bgAlpha << 24) & 0xFF000000;
        context.fill(0, 0, sw, sh, bgColor);

        // Blit the full texture region scaled into drawW x drawH.
        context.drawTexture(
                RenderLayer::getGuiTextured,
                TEXTURE_ID,
                x, y,
                0.0f, 0.0f,
                drawW, drawH,
                textureWidth, textureHeight,
                textureWidth, textureHeight
        );
    }

    /**
     * Dismisses the preview and frees the registered texture.
     */
    public void dismiss() {
        if (!active) {
            return;
        }
        active = false;
        textureWidth = 0;
        textureHeight = 0;
        if (currentTexture != null) {
            try {
                currentTexture.close();
            } catch (Exception e) {
                LOGGER.debug("Ignoring error closing preview texture: {}", e.getMessage());
            }
            currentTexture = null;
        }
    }

    public boolean isActive() {
        return active;
    }
}
