package studio.creaddinscart.imagepreviewer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.ImagePreviewerMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON-based configuration for the Image Previewer mod.
 * <p>
 * Stored at {@code .minecraft/config/imagepreviewer.json}. All fields have sensible
 * defaults and the file is created automatically on first launch if absent.
 * </p>
 *
 * <p>Usage:
 * <pre>{@code
 * ModConfig config = ModConfig.load(configPath);
 * int width = config.getMaxWidth();
 * config.setMaxWidth(600);
 * config.save(configPath);
 * }</pre>
 * </p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ModConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreviewerMod.MOD_ID + "/Config");

    /** Pretty-printing Gson instance for human-readable JSON output. */
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // ─── Configuration Fields ──────────────────────────────────────────────────

    /** Maximum width in pixels to scale the preview image. */
    @SerializedName("max_width")
    private int maxWidth = 400;

    /** Maximum height in pixels to scale the preview image. */
    @SerializedName("max_height")
    private int maxHeight = 300;

    /**
     * Opacity of the dark background overlay behind the image.
     * Range: 0.0 (transparent) to 1.0 (fully opaque).
     */
    @SerializedName("background_opacity")
    private float backgroundOpacity = 0.7f;

    /** Maximum allowed image download size in megabytes. */
    @SerializedName("max_image_size_mb")
    private int maxImageSizeMB = 10;

    /** Whether the image preview feature is enabled. */
    @SerializedName("enabled")
    private boolean enabled = true;

    /**
     * Hex color string used to highlight detected image URLs in chat.
     * Format: "#RRGGBB" (e.g., "#58a6ff").
     */
    @SerializedName("highlight_color")
    private String highlightColor = "#58a6ff";

    /** Whether to automatically show preview on hover (vs. requiring click). */
    @SerializedName("hover_preview")
    private boolean hoverPreview = false;

    /** Whether to show image dimensions and file size in the overlay. */
    @SerializedName("show_metadata")
    private boolean showMetadata = true;

    /** Whether to allow animated GIF playback (requires additional GL work). */
    @SerializedName("animate_gifs")
    private boolean animateGifs = false;

    // ─── Static Accessor ───────────────────────────────────────────────────────

    /** Singleton instance for quick access from any context. */
    private static ModConfig instance;

    /**
     * Returns the current active configuration singleton.
     * Must be initialized via {@link #load(Path)} before first access.
     *
     * @return the active ModConfig instance
     * @throws IllegalStateException if config has not been loaded yet
     */
    public static ModConfig get() {
        if (instance == null) {
            throw new IllegalStateException("ModConfig has not been loaded. Call ModConfig.load() first.");
        }
        return instance;
    }

    // ─── Load / Save ───────────────────────────────────────────────────────────

    /**
     * Loads configuration from the given file path.
     * <p>
     * If the file does not exist, a default configuration is created and written
     * to disk. If the file exists but cannot be parsed (corrupt JSON), the default
     * configuration is used and an error is logged.
     * </p>
     *
     * @param path the absolute path to the config JSON file
     * @return the loaded (or newly created) config instance
     */
    public static ModConfig load(Path path) {
        ModConfig config;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, ModConfig.class);
                if (config == null) {
                    LOGGER.warn("Config file is empty or invalid JSON — using defaults.");
                    config = new ModConfig();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read config file at {}: {} — using defaults.",
                        path, e.getMessage());
                config = new ModConfig();
            }
        } else {
            LOGGER.info("Config file not found at {} — creating with defaults.", path);
            config = new ModConfig();
            config.save(path);
        }

        // Clamp values to valid ranges
        config.maxWidth = Math.max(100, Math.min(1920, config.maxWidth));
        config.maxHeight = Math.max(100, Math.min(1080, config.maxHeight));
        config.backgroundOpacity = Math.max(0.0f, Math.min(1.0f, config.backgroundOpacity));
        config.maxImageSizeMB = Math.max(1, Math.min(100, config.maxImageSizeMB));

        // Validate hex color format
        if (config.highlightColor == null || !config.highlightColor.matches("#[0-9a-fA-F]{6}")) {
            config.highlightColor = "#58a6ff";
        }

        instance = config;
        return config;
    }

    /**
     * Saves the current configuration state to the given file path.
     * Creates parent directories if they don't exist.
     *
     * @param path the absolute path to write the config JSON
     */
    public void save(Path path) {
        try {
            // Ensure parent directory exists
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }

            LOGGER.debug("Config saved to {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to {}: {}", path, e.getMessage());
        }
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = Math.max(100, Math.min(1920, maxWidth));
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = Math.max(100, Math.min(1080, maxHeight));
    }

    public float getBackgroundOpacity() {
        return backgroundOpacity;
    }

    public void setBackgroundOpacity(float backgroundOpacity) {
        this.backgroundOpacity = Math.max(0.0f, Math.min(1.0f, backgroundOpacity));
    }

    public int getMaxImageSizeMB() {
        return maxImageSizeMB;
    }

    public void setMaxImageSizeMB(int maxImageSizeMB) {
        this.maxImageSizeMB = Math.max(1, Math.min(100, maxImageSizeMB));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(String highlightColor) {
        if (highlightColor != null && highlightColor.matches("#[0-9a-fA-F]{6}")) {
            this.highlightColor = highlightColor;
        }
    }

    public boolean isHoverPreview() {
        return hoverPreview;
    }

    public void setHoverPreview(boolean hoverPreview) {
        this.hoverPreview = hoverPreview;
    }

    public boolean isShowMetadata() {
        return showMetadata;
    }

    public void setShowMetadata(boolean showMetadata) {
        this.showMetadata = showMetadata;
    }

    public boolean isAnimateGifs() {
        return animateGifs;
    }

    public void setAnimateGifs(boolean animateGifs) {
        this.animateGifs = animateGifs;
    }

    /**
     * Parses the highlight color hex string into an integer RGB value.
     *
     * @return RGB color as an integer (0xRRGGBB)
     */
    public int getHighlightColorInt() {
        return Integer.parseInt(highlightColor.substring(1), 16);
    }
}
