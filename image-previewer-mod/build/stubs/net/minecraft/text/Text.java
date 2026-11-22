package net.minecraft.text;
public class Text {
    public static MutableText literal(String s) { return new MutableText(); }
    public static MutableText translatable(String key, Object... args) { return new MutableText(); }
    public static MutableText empty() { return new MutableText(); }
}
