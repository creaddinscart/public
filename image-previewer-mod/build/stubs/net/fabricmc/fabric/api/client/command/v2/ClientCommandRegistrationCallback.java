package net.fabricmc.fabric.api.client.command.v2;
public class ClientCommandRegistrationCallback {
    public static final Event EVENT = new Event();
    public static class Event {
        public void register(Callback callback) {}
        @FunctionalInterface
        public interface Callback {
            void register(FabricCommandDispatcher dispatcher, Object registryAccess);
        }
    }
    public static class FabricCommandDispatcher {
        public void register(Object command) {}
    }
}
