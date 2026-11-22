package net.minecraft.client.render;
public class BufferBuilder {
    public void begin(int mode, Object format) {}
    public VertexConsumer vertex(double x, double y, double z) { return new VertexConsumer(); }
    public void end() {}
}
