package studio.creaddinscart.imagepreviewer.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.ImagePreviewerMod;
import studio.creaddinscart.imagepreviewer.config.ModConfig;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Renders fetched images as a HUD overlay using native OpenGL.
 * <p>
 * Manages the full texture lifecycle: upload from {@link BufferedImage}, active
 * state tracking, per-frame rendering with scaling/centering, and cleanup on
 * context loss or dismissal.
 * </p>
 *
 * <p>All GL calls are marshalled to the render thread via
 * {@link RenderSystem#assertOnRenderThread()} to prevent race conditions.</p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ImageRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreviewerMod.MOD_ID + "/ImageRenderer");

    /** Global texture ID counter to avoid collisions with other mods. */
    private static final AtomicInteger TEXTURE_SEQ = new AtomicInteger(0);

    /** The mod's configuration for render parameters. */
    private final ModConfig config;

    /** OpenGL texture ID for the currently displayed image (0 = none). */
    private int textureId = 0;

    /** Width of the uploaded texture in pixels. */
    private int textureWidth = 0;

    /** Height of the uploaded texture in pixels. */
    private int textureHeight = 0;

    /** Whether an image overlay is currently active and should be rendered. */
    private volatile boolean active = false;

    /** Animation progress for fade-in effect (0.0 = invisible, 1.0 = fully visible). */
    private float fadeProgress = 0.0f;

    /** Target fade progress — set to 1.0 when displaying, 0.0 when dismissing. */
    private float fadeTarget = 0.0f;

    /** Speed of the fade animation (progress per frame at 60fps). */
    private static final float FADE_SPEED = 0.08f;

    public ImageRenderer(ModConfig config) {
        this.config = config;
    }

    /**
     * Uploads a {@link BufferedImage} as an OpenGL texture and activates the overlay.
     * <p>
     * Must be called from the render thread. Converts the image to RGBA format,
     * generates a GL texture, uploads pixel data, and marks the renderer as active.
     * Any previously displayed image is replaced.
     * </p>
     *
     * @param img the decoded image to display; must not be null
     */
    public void displayImage(BufferedImage img) {
        if (img == null) {
            LOGGER.warn("displayImage called with null image");
            return;
        }

        RenderSystem.assertOnRenderThread();

        // Clean up previous texture if one exists
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
        }

        // Convert to TYPE_INT_ARGB for consistent pixel access regardless of source format
        BufferedImage argb = ensureArgb(img);
        int width = argb.getWidth();
        int height = argb.getHeight();
        int[] pixels = ((DataBufferInt) argb.getRaster().getDataBuffer()).getData();

        // Pack pixels into a direct ByteBuffer in RGBA order (OpenGL expects R,G,B,A)
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));          // B
            buffer.put((byte) ((pixel >> 24) & 0xFF)); // A
        }
        buffer.flip();

        // Generate and bind the texture
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Set texture parameters — linear filtering for smooth scaling
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

        // Upload pixel data
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        // Store dimensions for render calculations
        this.textureWidth = width;
        this.textureHeight = height;

        // Activate the overlay with fade-in animation
        this.active = true;
        this.fadeProgress = 0.0f;
        this.fadeTarget = 1.0f;

        LOGGER.debug("Image uploaded as GL texture {}: {}x{}", textureId, width, height);
    }

    /**
     * Renders the active image overlay as part of the HUD draw pass.
     * <p>
     * Draws a semi-transparent dark background rectangle covering the full screen,
     * then renders the image texture centered and scaled to fit within the configured
     * maximum dimensions while preserving aspect ratio.
     * </p>
     *
     * @param context   the current draw context (provides access to the framebuffer size)
     * @param tickDelta partial tick for frame-rate-independent animation
     */
    public void renderOverlay(DrawContext context, float tickDelta) {
        if (!active || textureId == 0) {
            return;
        }

        RenderSystem.assertOnRenderThread();

        // Update fade animation
        updateFade();
        if (fadeProgress <= 0.0f && fadeTarget <= 0.0f) {
            // Fade-out complete — deactivate
            active = false;
            cleanupTexture();
            return;
        }

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        // Calculate scaled image dimensions preserving aspect ratio
        int maxW = config.getMaxWidth();
        int maxH = config.getMaxHeight();
        float scale = Math.min((float) maxW / textureWidth, (float) maxH / textureHeight);
        // Don't upscale small images beyond their native resolution
        scale = Math.min(scale, 1.0f);

        int drawWidth = (int) (textureWidth * scale);
        int drawHeight = (int) (textureHeight * scale);

        // Center on screen
        int x = (screenWidth - drawWidth) / 2;
        int y = (screenHeight - drawHeight) / 2;

        // ─── Draw semi-transparent background ───────────────────────────────────
        float bgAlpha = config.getBackgroundOpacity() * fadeProgress;
        int bgColor = ((int) (bgAlpha * 255) << 24); // Black with configurable alpha
        context.fill(0, 0, screenWidth, screenHeight, bgColor);

        // ─── Draw the image texture ─────────────────────────────────────────────
        drawTexturedQuad(context, x, y, drawWidth, drawHeight, fadeProgress);
    }

    /**
     * Draws a textured quad using the Tessellator for compatibility with
     * Minecraft's render pipeline (works with shaders and post-processing).
     *
     * @param context the draw context
     * @param x       screen X position
     * @param y       screen Y position
     * @param width   draw width in pixels
     * @param height  draw height in pixels
     * @param alpha   opacity multiplier (0.0–1.0)
     */
    private void drawTexturedQuad(DrawContext context, int x, int y, int width, int height, float alpha) {
        // Bind our texture to the active texture unit
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        // Configure render state for alpha blending
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        RenderSystem.setShaderTexture(0, textureId);

        // Use the Tessellator for batch-friendly rendering
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
        // Top-left
        buffer.vertex(x, y + height, 0).texture(0, 0).next();
        // Bottom-left
        buffer.vertex(x, y, 0).texture(0, 1).next();
        // Bottom-right
        buffer.vertex(x + width, y, 0).texture(1, 1).next();
        // Top-right
        buffer.vertex(x + width, y + height, 0).texture(1, 0).next();

        tessellator.draw();

        // Restore render state
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Dismisses the current image preview with a fade-out animation.
     * If no image is active, this is a no-op.
     */
    public void dismiss() {
        if (active) {
            fadeTarget = 0.0f;
            LOGGER.debug("Image overlay dismissed (fading out).");
        }
    }

    /**
     * Immediately removes the image without animation and frees the GL texture.
     * Called during context loss or shutdown.
     */
    public void dismissImmediate() {
        active = false;
        fadeProgress = 0.0f;
        cleanupTexture();
    }

    /**
     * Handles OpenGL context loss by resetting texture state.
     * The image data remains in the fetcher's cache and can be re-uploaded
     * when the context is restored.
     */
    public void onContextLost() {
        LOGGER.warn("GL context lost — resetting texture state.");
        textureId = 0;
        active = false;
        fadeProgress = 0.0f;
    }

    /**
     * Returns whether an image overlay is currently being displayed.
     *
     * @return true if an image is active (visible or fading)
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Advances the fade animation by one step.
     * Frame-rate independent using a fixed increment per render call.
     */
    private void updateFade() {
        if (fadeProgress < fadeTarget) {
            fadeProgress = Math.min(fadeProgress + FADE_SPEED, fadeTarget);
        } else if (fadeProgress > fadeTarget) {
            fadeProgress = Math.max(fadeProgress - FADE_SPEED, fadeTarget);
        }
    }

    /**
     * Deletes the current GL texture and resets state.
     */
    private void cleanupTexture() {
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
            textureWidth = 0;
            textureHeight = 0;
        }
    }

    /**
     * Converts a BufferedImage to TYPE_INT_ARGB format, ensuring consistent
     * pixel access regardless of the source image type (e.g., indexed, grayscale).
     *
     * @param img source image
     * @return ARGB-formatted image (may be the same instance if already correct format)
     */
    private BufferedImage ensureArgb(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_ARGB) {
            return img;
        }
        BufferedImage argb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        argb.getGraphics().drawImage(img, 0, 0, null);
        return argb;
    }
}
