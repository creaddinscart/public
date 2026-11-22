package org.lwjgl.opengl;
import java.nio.ByteBuffer;
public class GL11 {
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_TEXTURE_MIN_FILTER = 0x2800;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2801;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_CLAMP = 0x2900;
    public static final int GL_QUADS = 0x0007;
    public static int glGenTextures() { return 1; }
    public static void glBindTexture(int target, int tex) {}
    public static void glTexParameteri(int target, int pname, int param) {}
    public static void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels) {}
    public static void glDeleteTextures(int tex) {}
}
