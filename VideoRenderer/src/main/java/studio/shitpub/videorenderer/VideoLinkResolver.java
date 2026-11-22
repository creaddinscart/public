package studio.shitpub.videorenderer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

/**
 * Parses a shared video link and fetches the underlying media over the network.
 *
 * The player accepts a plain link; this resolver normalises it, follows redirects,
 * sniffs the response content-type and streams the bytes to a temporary file that
 * the decoder can read. Only formats the pure-Java decoder can handle (H.264 in an
 * MP4 / MOV / M4V container) are reported as playable.
 *
 * Built by CREADDINSCART STUDIO.
 */
public final class VideoLinkResolver {

    public enum Kind { DIRECT_VIDEO, UNKNOWN_CONTAINER, NOT_A_VIDEO, INVALID_URL }

    public record Resolved(Kind kind, Path file, String contentType, long bytes, String message) {}

    private VideoLinkResolver() {}

    public static Resolved resolve(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return new Resolved(Kind.INVALID_URL, null, "", 0, "Empty link.");
        }
        String trimmed = rawUrl.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            // Treat a bare host/path as an https link so pasted links "just work".
            trimmed = "https://" + trimmed;
        }
        try {
            URI uri = URI.create(trimmed);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return new Resolved(Kind.INVALID_URL, null, "", 0, "Could not parse the link.");
            }

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofMinutes(2))
                    .header("User-Agent", "VideoRenderer/1.0 (CREADDINSCART STUDIO)")
                    .header("Accept", "video/*,*/*")
                    .build();

            Path tmp = Files.createTempFile("videorenderer-", ".download");
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tmp));

            String contentType = response.headers().firstValue("content-type").orElse("");
            long bytes = Files.size(tmp);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);

            Kind kind = classify(contentType, path);
            String msg = switch (kind) {
                case DIRECT_VIDEO -> "Parsed " + bytes + " bytes (" + shortCt(contentType) + ").";
                case UNKNOWN_CONTAINER -> "Downloaded, but the container/type is not a playable H.264 MP4/MOV.";
                case NOT_A_VIDEO -> "The link did not return a video stream.";
                case INVALID_URL -> "Invalid link.";
            };
            return new Resolved(kind, tmp, contentType, bytes, msg);
        } catch (Exception e) {
            return new Resolved(Kind.INVALID_URL, null, "", 0, "Network error: " + e.getMessage());
        }
    }

    private static Kind classify(String contentType, String path) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        boolean videoByType = ct.startsWith("video/") || ct.contains("mp4") || ct.contains("quicktime");
        boolean videoByExt = path.endsWith(".mp4") || path.endsWith(".m4v")
                || path.endsWith(".mov") || path.endsWith(".mkv")
                || path.endsWith(".webm") || path.endsWith(".avi");
        if (videoByType || videoByExt) {
            // jcodec (pure Java) decodes H.264 in MP4/MOV/M4V. Other containers fail to open later.
            if (path.endsWith(".mkv") || path.endsWith(".webm") || path.endsWith(".avi")
                    || ct.contains("matroska") || ct.contains("webm")) {
                return Kind.UNKNOWN_CONTAINER;
            }
            return Kind.DIRECT_VIDEO;
        }
        if (ct.startsWith("text/html")) {
            return Kind.NOT_A_VIDEO;
        }
        return Kind.UNKNOWN_CONTAINER;
    }

    private static String shortCt(String contentType) {
        int semi = contentType.indexOf(';');
        return (semi > 0 ? contentType.substring(0, semi) : contentType).trim();
    }
}
