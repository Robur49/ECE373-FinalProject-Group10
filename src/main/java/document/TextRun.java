package document;

import java.util.Objects;

/** A contiguous piece of text with one formatting value. */
public final class TextRun {
    private final String text;
    private final TextStyle style;
    /** Creates a text run. */
    public TextRun(String text, TextStyle style) {
        this.text = Objects.requireNonNull(text, "text");
        this.style = Objects.requireNonNull(style, "style");
    }
    /** Returns the characters in this run. */ public String getText() { return text; }
    /** Returns the formatting value. */ public TextStyle getStyle() { return style; }
    /** Returns the number of characters. */ public int length() { return text.length(); }
}
