package studio.shitpub.videorenderer.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;

/**
 * Version shim for Minecraft 1.21 (Yarn, pre render-refactor API).
 *
 * Uses the legacy DrawContext.drawTexture overload (no RenderPipeline argument)
 * and the NativeImageBackedTexture(NativeImage) constructor.
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
        this.texture = new NativeImageBackedTexture(this.image);
        client.getTextureManager().registerTexture(this.id, this.texture);
    }

    public void update(BufferedImage frame) {
        if (frame == null) return;
        int fw = Math.min(width, frame.getWidth());
        int fh = Math.min(height, frame.getHeight());
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
                image.setColor(x, y, frame.getRGB(x, y));
            }
        }
        texture.upload();
    }

    public void draw(DrawContext context, int x, int y, int w, int h) {
        context.drawTexture(id, x, y, 0.0F, 0.0F, w, h, width, height);
    }

    public void release() {
        texture.close();
        image.close();
    }
}
