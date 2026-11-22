package com.creaddinscart.pixelchanger;
import net.minecraft.util.Identifier;
public final class PixelEditorScreen extends PixelEditorScreenBase {
    public PixelEditorScreen(Identifier s, PixelStore.Edit e){ super(s,e); }
    @Override public boolean mouseClicked(double x, double y, int b){ if(handleClick(x,y)) return true; return super.mouseClicked(x,y,b); }
    @Override public boolean mouseDragged(double x, double y, int b, double dx, double dy){ paintAt(x,y); return true; }
}
