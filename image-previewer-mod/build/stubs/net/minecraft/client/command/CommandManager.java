package net.minecraft.client.command;
public class CommandManager {
    public static LiteralCommandNode literal(String s) { return new LiteralCommandNode(); }
    public static class LiteralCommandNode {
        public LiteralCommandNode then(LiteralCommandNode l) { return this; }
        public LiteralCommandNode executes(Command callback) { return this; }
    }
    @FunctionalInterface
    public interface Command {
        int run(CommandContext ctx);
    }
    public static class CommandContext {
        public CommandSource getSource() { return new CommandSource(); }
    }
    public static class CommandSource {
        public void sendFeedback(Object text) {}
    }
}
