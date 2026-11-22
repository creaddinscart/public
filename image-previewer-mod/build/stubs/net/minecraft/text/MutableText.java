package net.minecraft.text;
import net.minecraft.util.Formatting;
import java.util.function.UnaryOperator;
public class MutableText extends Text {
    public MutableText append(Text t) { return this; }
    public MutableText formatted(Formatting... f) { return this; }
    public MutableText styled(UnaryOperator<Style> s) { return this; }
}
