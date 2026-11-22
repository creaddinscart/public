package com.creaddinscart.pixelchanger;

import net.minecraft.client.gui.Click;
import net.minecraft.util.Identifier;

/** Concrete editor for the new (1.21.6+) input API: mouse events arrive as {@link Click} records. */
public final class PixelEditorScreen extends PixelEditorScreenBase {

    public PixelEditorScreen(Identifier sprite, PixelStore.Edit edit) {
        super(sprite, edit);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (handleClick(click.x(), click.y())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        paintAt(click.x(), click.y());
        return true;
    }
}
