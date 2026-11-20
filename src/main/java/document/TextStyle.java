package document;

import java.util.Objects;

/** Immutable character formatting shared by text runs. */
public final class TextStyle {
    public static final TextStyle DEFAULT = new TextStyle(false, false, "Default", 12, "black");
    private final boolean bold;
    private final boolean italic;
    private final String fontName;
    private final int fontSize;
    private final String color;

    /** Creates a formatting value. */
    public TextStyle(boolean bold, boolean italic, String fontName, int fontSize, String color) {
        if (fontName == null || fontName.isBlank() || color == null || color.isBlank() || fontSize <= 0) {
            throw new IllegalArgumentException("Font, color, and positive font size are required");
        }
        this.bold = bold;
        this.italic = italic;
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.color = color;
    }
    /** Returns whether text is bold. */ public boolean isBold() { return bold; }
    /** Returns whether text is italic. */ public boolean isItalic() { return italic; }
    /** Returns the font name. */ public String getFontName() { return fontName; }
    /** Returns the font size. */ public int getFontSize() { return fontSize; }
    /** Returns the color. */ public String getColor() { return color; }
    /** Returns a copy with bold changed. */ public TextStyle withBold(boolean value) { return new TextStyle(value, italic, fontName, fontSize, color); }
    /** Returns a copy with italic changed. */ public TextStyle withItalic(boolean value) { return new TextStyle(bold, value, fontName, fontSize, color); }
    /** Returns a copy with font changed. */ public TextStyle withFontName(String value) { return new TextStyle(bold, italic, value, fontSize, color); }
    /** Returns a copy with size changed. */ public TextStyle withFontSize(int value) { return new TextStyle(bold, italic, fontName, value, color); }
    /** Returns a copy with color changed. */ public TextStyle withColor(String value) { return new TextStyle(bold, italic, fontName, fontSize, value); }
    @Override public boolean equals(Object other) {
        if (!(other instanceof TextStyle style)) return false;
        return bold == style.bold && italic == style.italic && fontSize == style.fontSize
                && fontName.equals(style.fontName) && color.equals(style.color);
    }
    @Override public int hashCode() { return Objects.hash(bold, italic, fontName, fontSize, color); }
}
