package studio.shitpub.videorenderer;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import studio.shitpub.videorenderer.render.KeybindCompat;

/**
 * Video Renderer - client entrypoint.
 *
 * Created by Btoom KtCher through CREADDINSCART STUDIO.
 * Built by CREADDINSCART STUDIO - https://www.shit.pub/studio/
 */
public class VideoRendererClient implements ClientModInitializer {

    public static final String MOD_ID = "videorenderer";
    public static final Logger LOGGER = LoggerFactory.getLogger("Video Renderer");

    private static KeyBinding openKey;
    private static boolean openKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Video Renderer initialising (Btoom KtCher / CREADDINSCART STUDIO).");

        openKey = KeyBindingHelper.registerKeyBinding(KeybindCompat.createOpenKey());

        // Open the player with the configured keybind (rising-edge via isPressed(),
        // which exists across every supported version unlike consumeClick/wasPressed).
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean down = openKey.isPressed();
            if (down && !openKeyWasDown) {
                openScreen(client, null);
            }
            openKeyWasDown = down;
        });

        // /video        -> open the player screen
        // /video <url>  -> open and immediately start playing the link
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("video")
                        .executes(ctx -> {
                            openScreen(ctx.getSource().getClient(), null);
                            return 1;
                        })
                        .then(ClientCommandManager.argument("url", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String url = StringArgumentType.getString(ctx, "url");
                                    openScreen(ctx.getSource().getClient(), url);
                                    return 1;
                                }))));

        // Add a "Video Renderer" button to the in-world pause menu so the player
        // can enter a world and open the viewer from there.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof GameMenuScreen) {
                ButtonWidget button = ButtonWidget.builder(Text.literal("Video Renderer"),
                                b -> openScreen(MinecraftClient.getInstance(), null))
                        .dimensions(scaledWidth / 2 + 60, scaledHeight / 4 + 96, 100, 20)
                        .build();
                @SuppressWarnings("unchecked")
                java.util.List<net.minecraft.client.gui.widget.ClickableWidget> buttons =
                        (java.util.List<net.minecraft.client.gui.widget.ClickableWidget>) Screens.getButtons(screen);
                buttons.add(button);
            }
        });
    }

    /** Opens the video player screen, optionally pre-loading a link. */
    public static void openScreen(MinecraftClient client, String url) {
        if (client != null) {
            client.setScreen(new VideoScreen(url));
        }
    }
}
