package net.minecraft.text;
public class HoverEvent<T> {
    public enum Action { SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY }
    public HoverEvent(Action action, T data) {}
}
