package net.minecraft.text;
import net.minecraft.util.Formatting;
public class Style {
    public Style withColor(Formatting f) { return this; }
    public Style withUnderline(boolean u) { return this; }
    public Style withClickEvent(ClickEvent e) { return this; }
    public Style withHoverEvent(HoverEvent<?> e) { return this; }
}
