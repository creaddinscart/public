package studio.shitpub.videorenderer;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * Decodes a downloaded video file into sequential frames using jcodec
 * (a pure-Java H.264 / MP4 decoder - no native libraries required).
 *
 * The first frame is read eagerly so the dimensions are known before the
 * texture is created; it is then returned as the first call to nextFrame().
 * Frames are pulled from the client tick loop, keeping the pipeline simple and
 * free of threading against Minecraft's render thread.
 *
 * Built by CREADDINSCART STUDIO.
 */
public final class VideoDecoder implements AutoCloseable {

    private final SeekableByteChannel channel;
    private final FrameGrab grab;
    private final int width;
    private final int height;
    private final double frameRate;

    private BufferedImage firstFrame;
    private boolean finished = false;

    private VideoDecoder(SeekableByteChannel channel, FrameGrab grab, int width, int height,
                         double frameRate, BufferedImage firstFrame) {
        this.channel = channel;
        this.grab = grab;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.firstFrame = firstFrame;
    }

    public static VideoDecoder open(Path file) throws Exception {
        SeekableByteChannel channel = NIOUtils.readableChannel(file.toFile());
        boolean ok = false;
        try {
            FrameGrab grab = FrameGrab.createFrameGrab(channel);
            Picture first = grab.getNativeFrame();
            if (first == null) {
                throw new IllegalStateException("No decodable video frames found.");
            }
            int w = first.getWidth();
            int h = first.getHeight();
            BufferedImage img = AWTUtil.toBufferedImage(first);
            double fps = 24.0; // jcodec's track meta does not reliably expose frame rate; assume a sane default.
            ok = true;
            return new VideoDecoder(channel, grab, w, h, fps, img);
        } finally {
            if (!ok) {
                NIOUtils.closeQuietly(channel);
            }
        }
    }

    public int width() { return width; }
    public int height() { return height; }
    public double frameRate() { return frameRate; }
    public boolean finished() { return finished; }

    /** Advances one frame and returns it, or null at end-of-stream / on error. */
    public BufferedImage nextFrame() {
        if (finished) return null;
        if (firstFrame != null) {
            BufferedImage f = firstFrame;
            firstFrame = null;
            return f;
        }
        try {
            Picture picture = grab.getNativeFrame();
            if (picture == null) {
                finished = true;
                return null;
            }
            return AWTUtil.toBufferedImage(picture);
        } catch (Exception | Error e) {
            // EOF or an undecodable tail -> treat the stream as finished.
            finished = true;
            VideoRendererClient.LOGGER.info("Decoder reached end of stream: {}", e.toString());
            return null;
        }
    }

    @Override
    public void close() {
        NIOUtils.closeQuietly(channel);
    }
}
