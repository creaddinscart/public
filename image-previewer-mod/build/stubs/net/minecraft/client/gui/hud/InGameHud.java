package net.minecraft.client.gui.hud;
public class InGameHud {
    public ChatHud getChatHud() { return new ChatHud(); }
    public static class ChatHud { public void addMessage(Object text) {} }
}
