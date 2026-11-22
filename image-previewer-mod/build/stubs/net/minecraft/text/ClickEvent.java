package net.minecraft.text;
public class ClickEvent {
    public enum Action { RUN_COMMAND, SUGGEST_COMMAND, OPEN_URL, OPEN_FILE, COPY_TO_CLIPBOARD }
    public ClickEvent(Action action, String value) {}
}
