package net.minecraft.client.render;
public class Tessellator {
    private static final Tessellator INSTANCE = new Tessellator();
    public static Tessellator getInstance() { return INSTANCE; }
    public BufferBuilder getBuffer() { return new BufferBuilder(); }
    public void draw() {}
}
