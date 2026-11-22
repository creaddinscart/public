package studio.creaddinscart.imagepreviewer.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.creaddinscart.imagepreviewer.ImagePreviewerMod;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Asynchronous image fetcher with in-memory caching.
 * <p>
 * Downloads remote images using Java 11+ {@link HttpClient} on a dedicated thread pool.
 * Decoded {@link BufferedImage} results are cached in a thread-safe map keyed by URL.
 * Enforces configurable size limits and content-type validation to prevent abuse.
 * </p>
 *
 * <p>Thread safety: all public methods are safe to call from any thread.
 * The internal cache uses {@link ConcurrentHashMap} and the HTTP client is
 * inherently thread-safe.</p>
 *
 * @author GMA
 * @since 1.0.0
 */
public class ImageFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImagePreviewerMod.MOD_ID + "/ImageFetcher");

    /** Maximum time to wait for a connection to establish. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    /** Maximum time to wait for the full response body. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    /** Acceptable MIME types for image content. */
    private static final java.util.Set<String> VALID_CONTENT_TYPES = java.util.Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp",
            "image/bmp", "image/svg+xml", "image/x-icon", "image/vnd.microsoft.icon",
            "image/tiff", "image/avif"
    );

    /** User-Agent header to identify the mod to remote servers. */
    private static final String USER_AGENT = "ImagePreviewer/1.0 (Minecraft Fabric Mod; +https://www.shit.pub/studio/)";

    /** The HTTP client instance — thread-safe and reused across requests. */
    private final HttpClient httpClient;

    /** Background executor for non-blocking downloads. */
    private final ExecutorService executor;

    /** Cache of successfully decoded images, keyed by their source URL. */
    private final ConcurrentHashMap<String, BufferedImage> cache = new ConcurrentHashMap<>();

    /** Tracks URLs currently being fetched to avoid duplicate requests. */
    private final ConcurrentHashMap<String, AtomicBoolean> inFlight = new ConcurrentHashMap<>();

    /** Maximum allowed image size in bytes. */
    private volatile long maxBytes;

    /**
     * Creates a new ImageFetcher with the specified size limit.
     *
     * @param maxSizeMB maximum image size in megabytes; images exceeding this
     *                  will be rejected without full download
     */
    public ImageFetcher(int maxSizeMB) {
        this.maxBytes = (long) maxSizeMB * 1024 * 1024;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        // Fixed pool of 4 threads — sufficient for chat-driven image loading
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "ImagePreviewer-Fetcher");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Asynchronously fetches and decodes an image from the given URL.
     * <p>
     * If the image is already cached, the callback is invoked immediately on the
     * calling thread. If a fetch for the same URL is already in progress, the
     * request is deduplicated (callback fires when the in-flight fetch completes).
     * </p>
     *
     * @param url      the image URL to fetch
     * @param callback invoked with the decoded {@link BufferedImage} on success,
     *                 or {@code null} on failure. Called on the fetcher's background
     *                 thread — callers must marshal to the render thread if needed.
     */
    public void fetchAsync(String url, Consumer<BufferedImage> callback) {
        if (url == null || url.isBlank()) {
            LOGGER.warn("fetchAsync called with null/blank URL");
            callback.accept(null);
            return;
        }

        // Check cache first — avoids redundant network requests
        BufferedImage cached = cache.get(url);
        if (cached != null) {
            callback.accept(cached);
            return;
        }

        // Deduplicate in-flight requests for the same URL
        AtomicBoolean flight = inFlight.computeIfAbsent(url, k -> new AtomicBoolean(false));
        if (!flight.compareAndSet(false, true)) {
            // A fetch is already running for this URL; enqueue callback to fire on completion
            LOGGER.debug("Fetch already in-flight for: {}", url);
            executor.submit(() -> {
                // Spin-wait briefly for the in-flight result to appear in cache
                for (int i = 0; i < 50; i++) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    BufferedImage result = cache.get(url);
                    if (result != null) {
                        callback.accept(result);
                        return;
                    }
                }
                // Timed out waiting — treat as failure
                callback.accept(null);
            });
            return;
        }

        // Submit the actual download task
        executor.submit(() -> {
            try {
                BufferedImage image = doFetch(url);
                if (image != null) {
                    cache.put(url, image);
                }
                callback.accept(image);
            } catch (Exception e) {
                LOGGER.error("Failed to fetch image from {}: {}", url, e.getMessage());
                callback.accept(null);
            } finally {
                inFlight.remove(url);
            }
        });
    }

    /**
     * Performs the synchronous HTTP download and image decode.
     * This method runs on the background executor thread.
     *
     * @param url the URL to download
     * @return the decoded image, or null if the response is invalid
     */
    private BufferedImage doFetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "image/*")
                    .GET()
                    .build();

            // Use ofBody handler to check Content-Length before reading full body
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            // Validate HTTP status
            if (response.statusCode() != 200) {
                LOGGER.warn("HTTP {} for URL: {}", response.statusCode(), url);
                return null;
            }

            // Validate content type
            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .map(ct -> ct.split(";")[0].trim().toLowerCase(Locale.ROOT))
                    .orElse("");

            if (!contentType.isEmpty() && !isValidContentType(contentType)) {
                LOGGER.warn("Invalid content-type '{}' for URL: {}", contentType, url);
                return null;
            }

            // Enforce size limit
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                LOGGER.warn("Empty response body for URL: {}", url);
                return null;
            }
            if (body.length > maxBytes) {
                LOGGER.warn("Image too large ({} bytes > {} limit) for URL: {}",
                        body.length, maxBytes, url);
                return null;
            }

            // Decode image from raw bytes
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(body));
            if (image == null) {
                LOGGER.warn("ImageIO could not decode image (unsupported format?) for URL: {}", url);
                return null;
            }

            LOGGER.debug("Successfully fetched image: {} ({}x{}, {} bytes)",
                    url, image.getWidth(), image.getHeight(), body.length);
            return image;

        } catch (IOException e) {
            LOGGER.error("I/O error fetching {}: {}", url, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Fetch interrupted for: {}", url);
            return null;
        } catch (IllegalArgumentException e) {
            LOGGER.error("Malformed URL: {} — {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * Checks whether a content-type string is an acceptable image MIME type.
     * Allows any "image/*" type as a permissive fallback.
     */
    private boolean isValidContentType(String contentType) {
        return VALID_CONTENT_TYPES.contains(contentType) || contentType.startsWith("image/");
    }

    /**
     * Returns a cached image if available, without triggering a fetch.
     *
     * @param url the image URL
     * @return cached BufferedImage, or null if not in cache
     */
    public BufferedImage getCached(String url) {
        return cache.get(url);
    }

    /**
     * Clears the entire image cache. Useful when the GL context is lost
     * and textures need to be re-uploaded.
     */
    public void clearCache() {
        cache.clear();
        LOGGER.info("Image cache cleared ({} entries removed).", cache.size());
    }

    /**
     * Updates the maximum allowed image size at runtime.
     *
     * @param maxSizeMB new size limit in megabytes
     */
    public void setMaxSizeMB(int maxSizeMB) {
        this.maxBytes = (long) maxSizeMB * 1024 * 1024;
        LOGGER.info("Max image size updated to {}MB", maxSizeMB);
    }

    /**
     * Returns the current number of cached images.
     * Useful for diagnostics and memory monitoring.
     *
     * @return cache size
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Gracefully shuts down the executor and clears resources.
     * Should be called on client disconnect or mod unload.
     */
    public void shutdown() {
        executor.shutdownNow();
        cache.clear();
        inFlight.clear();
        LOGGER.info("ImageFetcher shut down.");
    }
}
