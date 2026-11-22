package com.creaddinscart.pixelchanger;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Version-independent logic for the pixel editor: layout, palette legend, painting math and
 * rendering (DrawContext fill/text APIs are stable across the 1.21 line). The concrete
 * {@code PixelEditorScreen} in each texture-pipeline variant only wires up that version's
 * mouse/key input methods and forwards to {@link #handleClick} / {@link #paintAt}.
 */
public abstract class PixelEditorScreenBase extends Screen {

    protected final Identifier sprite;
    protected final PixelStore.Edit edit;

    protected TextFieldWidget colorField;

    protected int cell = 16;
    protected int gridX;
    protected int gridY;
    protected int gridW;
    protected int gridH;

    protected int brushIndex = 0;

    protected PixelEditorScreenBase(Identifier sprite, PixelStore.Edit edit) {
        super(Text.literal("Pixel Changer - " + sprite.getPath()));
        this.sprite = sprite;
        this.edit = edit;
    }

    @Override
    protected void init() {
        layout();
        int fieldX = gridX + 96;
        int fieldY = gridY + gridH + 14;
        colorField = new TextFieldWidget(this.textRenderer, fieldX, fieldY, 90, 18, Text.literal("0"));
        colorField.setText(Integer.toString(brushIndex));
        colorField.setChangedListener(s -> {
            int idx = parseIndex(s);
            if (idx >= 0) {
                brushIndex = idx;
            }
        });
        this.addSelectableChild(colorField);
        this.setInitialFocus(colorField);
    }

    private void layout() {
        int legendW = 168;
        int topPad = 28;
        int bottomPad = 46;
        int availW = Math.max(48, this.width - legendW - 44);
        int availH = Math.max(48, this.height - topPad - bottomPad);
        cell = Math.max(2, Math.min(availW / edit.width, availH / edit.height));
        cell = Math.min(cell, 26);
        gridW = edit.width * cell;
        gridH = edit.height * cell;
        gridX = 20;
        gridY = topPad;
    }

    private int parseIndex(String s) {
        try {
            int v = Integer.parseInt(s.trim());
            if (v >= 0 && v < ColorPalette.size()) {
                return v;
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xF0101418);

        context.drawCenteredTextWithShadow(this.textRenderer,
            "Pixel Changer - " + sprite.getPath(), this.width / 2, 8, 0xFFFFFF);

        context.fill(gridX - 1, gridY - 1, gridX + gridW + 1, gridY + gridH + 1, 0xFF3A3A3A);
        for (int y = 0; y < edit.height; y++) {
            for (int x = 0; x < edit.width; x++) {
                int argb = edit.pixels[y * edit.width + x] | 0xFF000000;
                int px = gridX + x * cell;
                int py = gridY + y * cell;
                context.fill(px, py, px + cell, py + cell, argb);
            }
        }

        int hx = (mouseX - gridX) / cell;
        int hy = (mouseY - gridY) / cell;
        if (hx >= 0 && hy >= 0 && hx < edit.width && hy < edit.height) {
            int px = gridX + hx * cell;
            int py = gridY + hy * cell;
            context.fill(px, py, px + cell, py + 1, 0xFFFFFFFF);
            context.fill(px, py + cell - 1, px + cell, py + cell, 0xFFFFFFFF);
            context.fill(px, py, px + 1, py + cell, 0xFFFFFFFF);
            context.fill(px + cell - 1, py, px + cell, py + cell, 0xFFFFFFFF);
        }

        renderPalette(context);
        renderHelp(context);

        if (colorField != null) {
            colorField.render(context, mouseX, mouseY, delta);
        }
    }

    private void renderPalette(DrawContext context) {
        int lx = gridX + gridW + 24;
        int ly = gridY;
        context.drawText(this.textRenderer, "Color sequence #", lx, ly - 13, 0xFFE0E0E0, false);
        List<ColorPalette.Entry> all = ColorPalette.all();
        int rowH = Math.max(10, Math.min(13, (this.height - ly - 26) / all.size()));
        for (int i = 0; i < all.size(); i++) {
            ColorPalette.Entry e = all.get(i);
            int ry = ly + i * rowH;
            int swatch = Math.min(rowH - 1, 10);
            context.fill(lx, ry, lx + swatch, ry + swatch, e.argb());
            context.fill(lx, ry, lx + swatch, ry + 1, 0xFF606060);
            String label = String.format("%2d  %s", i, e.name());
            int color = (i == brushIndex) ? 0xFFFFF07A : 0xFFCFCFCF;
            context.drawText(this.textRenderer, label, lx + swatch + 6, ry, color, false);
        }
    }

    private void renderHelp(DrawContext context) {
        int hy = gridY + gridH + 16;
        context.drawText(this.textRenderer, "Brush color #:", gridX, hy, 0xFFE0E0E0, false);
        String cur = String.format("-> %d  %s", brushIndex, ColorPalette.name(brushIndex));
        context.drawText(this.textRenderer, cur, gridX + 196, hy, ColorPalette.argb(brushIndex) | 0xFF000000, false);
        context.drawText(this.textRenderer,
            "Type a number, click pixels to paint. ESC applies & saves.",
            gridX, this.height - 15, 0xFFB8B8B8, false);
    }

    /** Paints the pixel under the cursor. Returns true if the click landed on the grid. */
    protected boolean handleClick(double mouseX, double mouseY) {
        if (mouseX >= gridX && mouseY >= gridY
            && mouseX < gridX + gridW && mouseY < gridY + gridH) {
            paintAt(mouseX, mouseY);
            return true;
        }
        return false;
    }

    protected void paintAt(double mouseX, double mouseY) {
        int hx = (int) ((mouseX - gridX) / cell);
        int hy = (int) ((mouseY - gridY) / cell);
        if (hx >= 0 && hy >= 0 && hx < edit.width && hy < edit.height) {
            PixelStore.setPixel(sprite, hx, hy, ColorPalette.argb(brushIndex) | 0xFF000000);
        }
    }

    @Override
    public void removed() {
        super.removed();
        PixelChangerClient.applyChanges();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
