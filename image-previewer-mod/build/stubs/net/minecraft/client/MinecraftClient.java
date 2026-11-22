package net.minecraft.client;
import net.minecraft.client.gui.hud.InGameHud;
public class MinecraftClient {
    public static MinecraftClient getInstance() { return new MinecraftClient(); }
    public java.io.File runDirectory = new java.io.File(".");
    public InGameHud inGameHud;
    public void execute(Runnable r) { r.run(); }
}
