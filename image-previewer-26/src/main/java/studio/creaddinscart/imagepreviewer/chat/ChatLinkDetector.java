package studio.creaddinscart.imagepreviewer.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects image URLs within Minecraft chat messages.
 * <p>
 * Uses a two-tier approach:
 * <ol>
 *   <li>Extension-based matching — URLs ending in known image file extensions</li>
 *   <li>Domain-based matching — URLs from known image hosting services that may
 *       not have explicit extensions (e.g., imgur album links)</li>
 * </ol>
 * </p>
 *
 * <p>This class is stateless and thread-safe.</p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ChatLinkDetector {

    /**
     * Pattern matching URLs with explicit image file extensions.
     * Captures the full URL including protocol (http/https).
     */
    private static final Pattern IMAGE_EXTENSION_PATTERN = Pattern.compile(
            "(https?://[^\\s<>\"'\\[\\]]+\\.(?:png|jpe?g|gif|webp|bmp|svg|tiff?|ico|avif))(?:\\?[^\\s<>\"'\\[\\]]*)?",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Pattern matching bare URLs (no extension) from known image hosting domains.
     * These services serve images directly even without a file extension in the URL path.
     */
    private static final Pattern IMAGE_HOST_PATTERN = Pattern.compile(
            "(https?://(?:i\\.)?(?:imgur\\.com|media\\.discordapp\\.net|cdn\\.discordapp\\.com"
                    + "|media-discordapp\\.cdn\\.discordapp\\.net"
                    + "|pbs\\.org|postimg\\.cc|i\\.postimg\\.cc"
                    + "|ibb\\.co|i\\.ibb\\.co"
                    + "|prntscr\\.com|sharex\\.io"
                    + "|catbox\\.moe|files\\.catbox\\.moe"
                    + "|litterbox\\.catbox\\.moe"
                    + "|transfer\\.sh|0x0\\.st"
                    + "|paste\\.ee|paste\\.gg)"
                    + "(?:/[^\\s<>\"'\\[\\]]*))",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * General URL pattern used to extract candidate URLs from mixed chat text.
     * Minecraft chat messages may contain surrounding text, formatting codes (§),
     * and click/hover event markup.
     */
    private static final Pattern URL_EXTRACT_PATTERN = Pattern.compile(
            "https?://[^\\s<>\"'\\[\\]]+",
            Pattern.CASE_INSENSITIVE
    );

    /** Set of image file extensions recognized by this detector. */
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "tiff", "tif", "ico", "avif"
    );

    /**
     * Scans a raw chat message string and returns all detected image URLs.
     * <p>
     * Results are deduplicated and returned in order of appearance.
     * Handles Minecraft formatting codes (§x) by stripping them before matching.
     * </p>
     *
     * @param message the raw chat message text (may contain formatting codes)
     * @return an unmodifiable list of detected image URLs, empty if none found
     */
    public List<String> detectImageUrls(String message) {
        if (message == null || message.isBlank()) {
            return Collections.emptyList();
        }

        // Strip Minecraft section formatting codes (§0-§f, §k-§o, §r)
        String cleaned = message.replaceAll("§[0-9a-fk-orA-FK-OR]", "");

        List<String> results = new ArrayList<>();

        // Pass 1: URLs with explicit image extensions
        Matcher extMatcher = IMAGE_EXTENSION_PATTERN.matcher(cleaned);
        while (extMatcher.find()) {
            String url = extMatcher.group(1);
            if (!results.contains(url)) {
                results.add(url);
            }
        }

        // Pass 2: URLs from known image hosting domains (no extension required)
        Matcher hostMatcher = IMAGE_HOST_PATTERN.matcher(cleaned);
        while (hostMatcher.find()) {
            String url = hostMatcher.group(1);
            // Avoid duplicates from pass 1 (some imgur URLs also have extensions)
            if (!results.contains(url) && isImageUrl(url)) {
                results.add(url);
            }
        }

        return Collections.unmodifiableList(results);
    }

    /**
     * Validates whether a single URL string points to an image resource.
     * <p>
     * Checks both the file extension and domain against known image sources.
     * This can be used as a secondary validation after extraction.
     * </p>
     *
     * @param url the URL to validate
     * @return {@code true} if the URL is likely an image resource
     */
    public boolean isImageUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        // Normalize: strip query parameters and fragments for extension check
        String path = url.split("[?#]")[0].toLowerCase();

        // Check explicit extension
        int lastDot = path.lastIndexOf('.');
        if (lastDot > path.lastIndexOf('/')) {
            String ext = path.substring(lastDot + 1);
            if (SUPPORTED_EXTENSIONS.contains(ext)) {
                return true;
            }
        }

        // Check known image hosting domains
        return IMAGE_HOST_PATTERN.matcher(url).matches();
    }

    /**
     * Extracts all URLs from a message regardless of whether they point to images.
     * Useful for debugging or for applying click events to non-image URLs.
     *
     * @param message the raw chat message text
     * @return list of all URLs found in the message
     */
    public List<String> extractAllUrls(String message) {
        if (message == null || message.isBlank()) {
            return Collections.emptyList();
        }

        String cleaned = message.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
        List<String> urls = new ArrayList<>();

        Matcher matcher = URL_EXTRACT_PATTERN.matcher(cleaned);
        while (matcher.find()) {
            String url = matcher.group();
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }

        return Collections.unmodifiableList(urls);
    }

    /**
     * Returns the detected file extension for an image URL, or empty string if unknown.
     *
     * @param url the image URL
     * @return lowercase extension without the dot (e.g., "png", "jpeg"), or ""
     */
    public String getExtension(String url) {
        if (url == null) return "";
        String path = url.split("[?#]")[0].toLowerCase();
        int lastDot = path.lastIndexOf('.');
        if (lastDot > path.lastIndexOf('/')) {
            return path.substring(lastDot + 1);
        }
        return "";
    }
}
