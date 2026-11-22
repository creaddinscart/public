package com.creaddinscart.pixelchanger;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Holds the per-texture pixel edits (ARGB) and their on-disk persistence.
 * Textures are decoded/encoded as PNG with plain Java ImageIO so this layer is
 * completely independent of Minecraft's internal NativeImage across versions.
 */
public final class PixelStore {

    public static final class Edit {
        public final int width;
        public final int height;
        public final int[] pixels; // ARGB, row-major
        public boolean dirty;

        Edit(int width, int height, int[] pixels) {
            this.width = width;
            this.height = height;
            this.pixels = pixels;
        }
    }

    private static final Map<String, Edit> EDITS = new LinkedHashMap<>();
    private static Path saveFile;

    private PixelStore() {
    }

    public static void init(Path runDir) {
        Path dir = runDir.resolve("pixelchanger");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            PixelChanger.LOGGER.warn("Could not create Pixel Changer data directory", e);
        }
        saveFile = dir.resolve("edits.json");
        load();
    }

    public static Edit edit(String textureId) {
        return EDITS.get(textureId);
    }

    /** Returns the edit for a texture, loading the original PNG from the resource manager if unseen. */
    public static Edit getOrLoad(Identifier textureId, ResourceManager rm) {
        String key = textureId.toString();
        Edit existing = EDITS.get(key);
        if (existing != null) {
            return existing;
        }
        try {
            Optional<Resource> res = rm.getResource(textureId);
            if (res.isEmpty()) {
                return null;
            }
            BufferedImage img;
            try (InputStream in = res.get().getInputStream()) {
                img = ImageIO.read(in);
            }
            if (img == null) {
                return null;
            }
            int w = img.getWidth();
            int h = img.getHeight();
            int[] px = new int[w * h];
            img.getRGB(0, 0, w, h, px, 0, w);
            Edit e = new Edit(w, h, px);
            EDITS.put(key, e);
            return e;
        } catch (IOException e) {
            PixelChanger.LOGGER.warn("Failed to read texture {}", key, e);
            return null;
        }
    }

    public static void setPixel(Identifier textureId, int x, int y, int argb) {
        Edit e = EDITS.get(textureId.toString());
        if (e == null || x < 0 || y < 0 || x >= e.width || y >= e.height) {
            return;
        }
        e.pixels[y * e.width + x] = argb;
        e.dirty = true;
    }

    /** Clears a single texture's edits back to a solid color (used by the Clear button). */
    public static void fill(Identifier textureId, int argb) {
        Edit e = EDITS.get(textureId.toString());
        if (e == null) {
            return;
        }
        java.util.Arrays.fill(e.pixels, argb);
        e.dirty = true;
    }

    public static byte[] encodePng(Identifier textureId) throws IOException {
        Edit e = EDITS.get(textureId.toString());
        if (e == null) {
            return null;
        }
        BufferedImage img = new BufferedImage(e.width, e.height, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, e.width, e.height, e.pixels, 0, e.width);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    public static boolean hasAnyDirty() {
        for (Edit e : EDITS.values()) {
            if (e.dirty) {
                return true;
            }
        }
        return false;
    }

    public static void clearDirty() {
        for (Edit e : EDITS.values()) {
            e.dirty = false;
        }
    }

    public static Set<String> editedTextureIds() {
        return EDITS.keySet();
    }

    public static void save() {
        if (saveFile == null) {
            return;
        }
        JsonObject root = new JsonObject();
        for (Map.Entry<String, Edit> en : EDITS.entrySet()) {
            Edit e = en.getValue();
            JsonObject o = new JsonObject();
            o.addProperty("width", e.width);
            o.addProperty("height", e.height);
            JsonArray arr = new JsonArray();
            for (int p : e.pixels) {
                arr.add(p & 0xFFFFFFFFL);
            }
            o.add("pixels", arr);
            root.add(en.getKey(), o);
        }
        try {
            Files.writeString(saveFile, new GsonBuilder().create().toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            PixelChanger.LOGGER.warn("Failed to save Pixel Changer edits", ex);
        }
    }

    private static void load() {
        if (saveFile == null || !Files.exists(saveFile)) {
            return;
        }
        try {
            String s = Files.readString(saveFile, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(s).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> en : root.entrySet()) {
                JsonObject o = en.getValue().getAsJsonObject();
                int w = o.get("width").getAsInt();
                int h = o.get("height").getAsInt();
                JsonArray arr = o.getAsJsonArray("pixels");
                int[] px = new int[w * h];
                int i = 0;
                for (var el : arr) {
                    if (i < px.length) {
                        px[i++] = (int) el.getAsLong();
                    }
                }
                Edit e = new Edit(w, h, px);
                e.dirty = false;
                EDITS.put(en.getKey(), e);
            }
            PixelChanger.LOGGER.info("Loaded {} edited texture(s) from previous session", EDITS.size());
        } catch (Exception ex) {
            PixelChanger.LOGGER.warn("Failed to load Pixel Changer edits", ex);
        }
    }
}
