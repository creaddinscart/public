# Video Renderer

Watch and preview web videos **inside Minecraft**. Pick a world, paste a video link,
and the mod auto-parses it over the network, decodes it, and renders the stream in-game.

Created by **Btoom KtCher** through **CREADDINSCART STUDIO**.
**Built by CREADDINSCART STUDIO** - https://www.shit.pub/studio/

- **License:** MIT
- **Language:** English
- **Mod type:** Fabric client mod
- **Supported Minecraft versions:** 1.21, 1.21.10, 1.21.11 (one jar per version)

---

## Install

1. Install **Fabric Loader** for your Minecraft version.
2. Install the **Fabric API** for that same Minecraft version.
3. Drop the matching jar into your `mods` folder:
   - `videorenderer-1.21-1.0.0.jar` for Minecraft 1.21
   - `videorenderer-1.21.10-1.0.0.jar` for Minecraft 1.21.10
   - `videorenderer-1.21.11-1.0.0.jar` for Minecraft 1.21.11

Each jar is pinned to exactly one Minecraft version, so use the one that matches.

## Usage

Once in a world, open the player in any of these ways:

- Press **V** (rebindable under *Controls > Video Renderer*).
- Run `/video` to open the screen, or `/video <url>` to open and start immediately.
- Click the **Video Renderer** button added to the in-world pause menu.

Paste a video link into the box and press **Play**. The mod:

1. Resolves the link and follows redirects.
2. Downloads the media over the network to a temporary file.
3. Decodes it with the bundled pure-Java decoder (no native libraries).
4. Renders decoded frames onto the screen, advancing one frame per tick at the video's rate.

**Stop** releases the current stream; **Done** closes the screen.

---

## Honest limitations

This is a genuine, loadable mod, but the "watch any video link" feature has real technical bounds
that you should know about before relying on it:

- **Playable formats:** direct links to **H.264 video in an MP4 / MOV / M4V container** (a plain
  `https://.../clip.mp4`). The decoder is jcodec, a pure-Java H.264 demuxer/decoder.
- **Not decoded:** WebM / VP9 / AV1 / Matroska (`.mkv`, `.webm`) and other codecs. The mod detects
  these and reports that the container is not playable rather than crashing.
- **Share-page links are not extracted.** A link to a video *hosting page* (e.g. a YouTube watch
  page) is an HTML page, not a media stream, so it is reported as "not a video stream". Only URLs
  that return the actual media bytes (direct file links, or a server that redirects to them) play.
- **Video only - no audio.** Frames are rendered; audio playback is intentionally out of scope.
- **Frame timing** is approximate (a default 24 fps assumption) because the container's frame rate
  is not always reliably exposed by the decoder.

## Build from source

Requires JDK 21+ (the build emits `--release 21` bytecode) and Gradle (wrapper included).
The toolchain uses Fabric Loom 1.15.5 and Yarn mappings.

Build one jar per Minecraft version:

```bash
# 1.21.11 (default)
./gradlew build

# 1.21.10
./gradlew build -PmcVersion=1.21.10 -PyarnVersion=1.21.10+build.3 \
  -PfabricVersion=0.138.4+1.21.10 -PshimDir=v1_21_10

# 1.21
./gradlew build -PmcVersion=1.21 -PyarnVersion=1.21+build.9 \
  -PfabricVersion=0.102.0+1.21 -PshimDir=v1_21
```

Output jars land in `build/libs/`. Version-specific Minecraft render / keybind APIs are isolated in
`src/shim/<version>/` so the shared player code stays version independent.

## Layout

```
src/main/java/studio/shitpub/videorenderer/
  VideoRendererClient.java   client entrypoint: keybind, /video command, pause-menu button
  VideoScreen.java           the player screen (URL box, async load, tick-driven playback)
  VideoLinkResolver.java     link parsing + network fetch + content sniffing
  VideoDecoder.java          jcodec frame extraction
src/shim/<version>/.../render/
  VideoTextureRenderer.java  per-version DynamicTexture + blit
  KeybindCompat.java         per-version KeyBinding construction
src/main/resources/
  fabric.mod.json            metadata (authors, MIT, icon, per-version dependency)
  assets/videorenderer/      icon.png + lang/en_us.json
```

---

MIT License. Copyright (c) 2026 Btoom KtCher / CREADDINSCART STUDIO.
Built by CREADDINSCART STUDIO - https://www.shit.pub/studio/
