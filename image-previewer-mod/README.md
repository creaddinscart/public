# Image Previewer

`v1.0.0` · `MIT` · `Minecraft 1.16.5+` · `Fabric`

**Renders linked images directly in-game from chat messages.**

---

## What It Does

Image Previewer detects image URLs in Minecraft chat and lets you preview them without leaving the game. Simply click any detected image link and the image is downloaded and displayed as a HUD overlay right on your screen.

- Auto-detects image URLs in all chat messages (server, player, system)
- Supports URLs with file extensions and known image hosting services
- Click-to-preview with smooth fade-in/fade-out animation
- Semi-transparent dark background for readability
- In-memory caching — repeat views are instant
- Configurable size limits, colors, and behavior

## How to Use

1. **Install** — Place the `.jar` file into your `.minecraft/mods/` folder.
2. **Launch** — Start Minecraft with the Fabric loader and Fabric API.
3. **Chat** — When an image URL appears in chat, it will be highlighted in cyan with an underline.
4. **Click** — Left-click the URL to preview the image in a centered overlay.
5. **Dismiss** — Press `Escape` or run `/imagepreviewer dismiss` to close the preview.

### Commands

| Command | Description |
|---------|-------------|
| `/imagepreviewer reload` | Reload configuration from disk |
| `/imagepreviewer dismiss` | Close the current image preview |
| `/imagepreviewer toggle` | Enable or disable the mod |

## Supported Formats

| Format | Extension | Notes |
|--------|-----------|-------|
| PNG | `.png` | Full support |
| JPEG | `.jpg`, `.jpeg` | Full support |
| GIF | `.gif` | Static frames (animated GIFs opt-in via config) |
| WebP | `.webp` | Requires Java ImageIO plugin |
| BMP | `.bmp` | Full support |
| TIFF | `.tif`, `.tiff` | Requires Java ImageIO plugin |
| ICO | `.ico` | Full support |
| SVG | `.svg` | Limited (rasterized via ImageIO) |

### Supported Hosting Services

URLs from these services are detected even without a file extension:

- imgur.com / i.imgur.com
- media.discordapp.net / cdn.discordapp.com
- pbs.org
- postimg.cc / i.postimg.cc
- ibb.co / i.ibb.co
- catbox.moe / files.catbox.moe
- litterbox.catbox.moe
- prntscr.com
- transfer.sh / 0x0.st
- paste.ee / paste.gg

## Configuration

Config file: `.minecraft/config/imagepreviewer.json`

```json
{
  "max_width": 400,
  "max_height": 300,
  "background_opacity": 0.7,
  "max_image_size_mb": 10,
  "enabled": true,
  "highlight_color": "#58a6ff",
  "hover_preview": false,
  "show_metadata": true,
  "animate_gifs": false
}
```

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `max_width` | int | 400 | Maximum preview width in pixels |
| `max_height` | int | 300 | Maximum preview height in pixels |
| `background_opacity` | float | 0.7 | Dark overlay behind image (0.0–1.0) |
| `max_image_size_mb` | int | 10 | Maximum download size before rejection |
| `enabled` | boolean | true | Master enable/disable toggle |
| `highlight_color` | string | `#58a6ff` | Hex color for highlighted URLs |
| `hover_preview` | boolean | false | Show preview on hover (no click needed) |
| `show_metadata` | boolean | true | Display dimensions and size in overlay |
| `animate_gifs` | boolean | false | Enable animated GIF playback |

## Building from Source

```bash
# Clone the repository
git clone https://github.com/creaddinscart/imagepreviewer.git
cd imagepreviewer

# Build (requires JDK 17+)
./gradlew build

# Output jar: build/libs/imagepreviewer-1.0.0.jar
```

The built jar can be found in `build/libs/`. Install it in your `mods/` folder alongside Fabric API.

### Prerequisites

- Java Development Kit 17 or later
- Gradle (wrapper included)
- Internet access (for dependency resolution on first build)

## Credits

- **Developer:** GMA
- **Studio:** CREADDINSCART STUDIO
- **Homepage:** [shit.pub/studio](https://www.shit.pub/studio/)

## License

MIT License — Copyright (c) 2026 CREADDINSCART STUDIO (GMA)

Authorized and protected via [https://www.shit.pub/studio/](https://www.shit.pub/studio/)

## Version History

| Version | Date | Changes |
|---------|------|---------|
| **1.0.0** | 2026-11-21 | Initial release — click-to-preview, multi-format support, JSON config, HUD overlay rendering, URL caching |
