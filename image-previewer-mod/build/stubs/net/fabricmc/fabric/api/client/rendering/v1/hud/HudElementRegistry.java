package net.fabricmc.fabric.api.client.rendering.v1.hud;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.DrawContext;
public class HudElementRegistry {
    public static void addLast(Identifier id, HudElement element) {}
    public interface HudElement { void render(DrawContext ctx, float tickDelta); }
}
