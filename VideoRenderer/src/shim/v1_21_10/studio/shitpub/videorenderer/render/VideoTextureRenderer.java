package studio.shitpub.videorenderer.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;

/**
 * Version shim for Minecraft 1.21.11 (Yarn, post render-refactor API).
 *
 * Owns the NativeImageBackedTexture lifecycle and the DrawContext.drawTexture
 * call so the shared player code stays version independent. Only this file
 * changes between targets.
 *
 * Built by CREADDINSCART STUDIO.
 */
public final class VideoTextureRenderer {

    private final MinecraftClient client;
    private final int width;
    private final int height;
    private final Identifier id;
    private final NativeImage image;
    private final NativeImageBackedTexture texture;

    public VideoTextureRenderer(MinecraftClient client, int width, int height) {
        this.client = client;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.id = Identifier.of("videorenderer", "video_frame_" + System.nanoTime());
        this.image = new NativeImage(this.width, this.height, false);
        this.texture = new NativeImageBackedTexture(() -> "videorenderer video frame", this.image);
        client.getTextureManager().registerTexture(this.id, this.texture);
    }

    public void update(BufferedImage frame) {
        if (frame == null) return;
        int fw = Math.min(width, frame.getWidth());
        int fh = Math.min(height, frame.getHeight());
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
                image.setColorArgb(x, y, frame.getRGB(x, y));
            }
        }
        texture.upload();
    }

    public void draw(DrawContext context, int x, int y, int w, int h) {
        context.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 0.0F, 0.0F, w, h, width, height);
    }

    public void release() {
        texture.close();
        image.close();
    }
}
