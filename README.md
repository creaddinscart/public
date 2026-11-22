# Pixel Changer

A live, in-world **block texture editor** for Minecraft (Fabric).

Aim at any block, press **F8**, and the block's texture pops open as a grid of individual pixels.
Pick a brush by typing a **color sequence number**, then click / drag to repaint the block pixel by
pixel. Close the editor (ESC) and your edits are applied to the block in the world and saved to disk.

> Built by **Maker SX** through **CREADDINSCART STUDIO** — https://www.shit.pub/studio/

---

## Supported versions

| Minecraft | File |
|-----------|------|
| 1.21      | `PixelChanger-1.21-1.0.0.jar` |
| 1.21.10   | `PixelChanger-1.21.10-1.0.0.jar` |
| 1.21.11   | `PixelChanger-1.21.11-1.0.0.jar` |

Pick the jar that matches your exact Minecraft version. Each jar is pinned to one version.

## Requirements

- **Fabric Loader** (0.15.0+)
- **Fabric API** (matching your Minecraft version)
- Java 21+

## Install

1. Install Fabric Loader for your Minecraft version.
2. Drop the matching Fabric API jar **and** the matching `PixelChanger-*.jar` into your `.minecraft/mods` folder.
3. Launch the game, enter (select) a world.

## Usage

1. Walk up to a block and look at it (crosshair on the block).
2. Press **F8** to open the **Pixel Editor** for that block's texture.
3. On the right is the **color sequence** palette, each color with a number (0–31).
   Type a number in the **Brush color #** field to choose that color.
4. **Click** (or click-drag) a pixel in the grid to paint it with the current brush color.
5. Press **ESC** to apply and save. The block now renders with your painted texture, in-world.

Edits are stored in `.minecraft/pixelchanger/edits.json` and are re-applied automatically after a
resource reload (F3+T, rejoining a world, changing resource packs, ...).

Notes / limits:
- This is a **client-side** mod; it changes how blocks look for you.
- The editor targets the block's main texture (full-cube blocks such as stone, dirt, planks, ores,
  etc. work best). Multi-face blocks (e.g. grass, logs) edit one resolved face texture.

## Building from source

The project is a Gradle multi-module build that compiles one shared core against three Minecraft
versions (the input and texture-pipeline APIs diverged across the 1.21 line, so the editor screen and
atlas patcher are per-version variants).

```
./gradlew build
```

Artifacts land in `v121/build/libs`, `v12110/build/libs`, `v12111/build/libs`.

Layout:
- `common/src/main/java/...` — version-independent core: `PixelStore`, `ColorPalette`,
  `BlockTextures`, `PixelChangerClient` (keybind + tick driver), `TexturePatch`, `PixelEditorScreenBase`.
- `v121/`  — 1.21.0 variant (old OpenGL texture pipeline + String keybind category + double-based mouse API).
- `v12110/` — 1.21.10 variant (new GPU pipeline, public per-sprite `upload`, `Click` input API).
- `v12111/` — 1.21.11 variant (new GPU pipeline, full-atlas `upload()` re-blit, `Click` input API).
- `*/src/main/resources/pixelchanger.accesswidener` — widens the private `SpriteContents.image`
  (and `SpriteAtlasTexture.upload()` on 1.21.11) so edits can be written into the block atlas.

## License

MIT © 2026 CREADDINSCART STUDIO — Pixel Changer, built by Maker SX.
Home: https://www.shit.pub/studio/
