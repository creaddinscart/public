package studio.shitpub.videorenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import studio.shitpub.videorenderer.render.VideoTextureRenderer;

import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * The in-game video player screen.
 *
 * Flow: enter a world -> open this screen (keybind, /video, or the pause-menu
 * button) -> paste a video link -> the link is auto-parsed over the network,
 * decoded with the bundled pure-Java decoder, and rendered frame by frame.
 *
 * Created by Btoom KtCher through CREADDINSCART STUDIO.
 * Built by CREADDINSCART STUDIO - https://www.shit.pub/studio/
 */
public class VideoScreen extends Screen {

    private final String pendingUrl;

    private TextFieldWidget urlField;

    private volatile boolean loading = false;
    private volatile String status = "Paste a video link and press Play.";

    private volatile VideoDecoder pendingDecoder;

    private VideoDecoder decoder;
    private VideoTextureRenderer renderer;

    private long nextFrameNanos = 0L;

    public VideoScreen(String initialUrl) {
        super(Text.literal("Video Renderer"));
        this.pendingUrl = initialUrl;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.urlField = new TextFieldWidget(this.textRenderer, cx - 160, cy - 90, 320, 20,
                Text.literal("Video URL"));
        this.urlField.setPlaceholder(Text.literal("https://example.com/clip.mp4"));
        if (this.pendingUrl != null) {
            this.urlField.setText(this.pendingUrl);
        }
        this.addDrawableChild(this.urlField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Play"), button -> start(this.urlField.getText()))
                .dimensions(cx - 160, cy - 66, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), button -> stop())
                .dimensions(cx - 76, cy - 66, 60, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> this.close())
                .dimensions(cx + 100, cy - 66, 60, 20).build());

        this.setInitialFocus(this.urlField);
    }

    private void start(String url) {
        if (loading) return;
        stop();
        loading = true;
        status = "Resolving link over the network...";
        Thread worker = new Thread(() -> {
            VideoLinkResolver.Resolved resolved = VideoLinkResolver.resolve(url);
            if (resolved.kind() == VideoLinkResolver.Kind.DIRECT_VIDEO) {
                try {
                    this.pendingDecoder = VideoDecoder.open(resolved.file());
                } catch (Throwable t) {
                    this.status = "Could not decode: " + t.getMessage();
                }
            } else {
                this.status = resolved.message();
            }
            this.loading = false;
        }, "VideoRenderer-Load");
        worker.setDaemon(true);
        worker.start();
    }

    private void stop() {
        if (this.renderer != null) {
            this.renderer.release();
            this.renderer = null;
        }
        if (this.decoder != null) {
            this.decoder.close();
            this.decoder = null;
        }
        if (this.pendingDecoder != null) {
            this.pendingDecoder.close();
            this.pendingDecoder = null;
        }
        this.nextFrameNanos = 0L;
    }

    @Override
    public void tick() {
        super.tick();

        // Pick up an asynchronously resolved decoder on the client thread.
        VideoDecoder ready = this.pendingDecoder;
        if (ready != null) {
            this.decoder = ready;
            this.pendingDecoder = null;
            this.renderer = new VideoTextureRenderer(MinecraftClient.getInstance(), ready.width(), ready.height());
            this.status = "Playing " + ready.width() + "x" + ready.height()
                    + " @ " + String.format(Locale.ROOT, "%.1f", ready.frameRate()) + " fps";
            this.nextFrameNanos = System.nanoTime();
        }

        // Advance one frame when its display time elapses.
        if (this.decoder != null && this.renderer != null && !this.decoder.finished()) {
            long now = System.nanoTime();
            if (now >= this.nextFrameNanos) {
                BufferedImage frame = this.decoder.nextFrame();
                if (frame != null) {
                    this.renderer.update(frame);
                    double fps = Math.max(1.0, this.decoder.frameRate());
                    this.nextFrameNanos = now + (long) (1_000_000_000L / fps);
                } else {
                    this.status = "End of stream.";
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int top = this.height / 2 - 50;

        if (this.renderer != null && this.decoder != null) {
            int maxW = 320, maxH = 180;
            double ar = (double) this.decoder.width() / (double) Math.max(1, this.decoder.height());
            int drawW = maxW;
            int drawH = (int) (drawW / ar);
            if (drawH > maxH) {
                drawH = maxH;
                drawW = (int) (drawH * ar);
            }
            int x = cx - drawW / 2;
            context.fill(x - 2, top - 2, x + drawW + 2, top + drawH + 2, 0xFF101018);
            this.renderer.draw(context, x, top, drawW, drawH);
        }

        context.drawCenteredTextWithShadow(this.textRenderer,
                "Video Renderer - Btoom KtCher / CREADDINSCART STUDIO",
                cx, this.height - 28, 0xFFB8C0E0);
        context.drawCenteredTextWithShadow(this.textRenderer, this.status, cx, top + 190, 0xFFFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        stop();
        super.close();
    }
}
