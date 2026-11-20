//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package model;

import java.util.Objects;

// Stores the bold, italic, font, size, and color settings for text.
public class TextStyle {

    public static final TextStyle DEFAULT = new TextStyle(false, false, "Monospaced", 12, "black"); // Starting style for text that has not been formatted.

    private final boolean bold;
    private final boolean italic;
    private final String fontName;
    private final int fontSize;
    private final String color;

    public TextStyle(boolean bold, boolean italic, String fontName, int fontSize, String color) {
        if (fontName == null || fontName.isBlank()) {
            throw new IllegalArgumentException("Font name cannot be blank");
        }
        if (fontSize < 1 || fontSize > 200) {
            throw new IllegalArgumentException("Font size must be between 1 and 200");
        }
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("Color cannot be blank");
        }

        this.bold = bold; // Save whether this style uses bold text.
        this.italic = italic; // Save whether this style uses italic text.
        this.fontName = fontName; // Keep the font chosen by the user.
        this.fontSize = fontSize; // Keep the font size for this style.
        this.color = color; // Keep the selected text color.
    }

    public boolean bold() {
        return bold;
    }

    public boolean italic() {
        return italic;
    }

    public String fontName() {
        return fontName;
    }

    public int fontSize() {
        return fontSize;
    }

    public String color() {
        return color;
    }

    public TextStyle with(String property, String value) {
        if (property == null || value == null) {
            throw new IllegalArgumentException("Formatting option cannot be null");
        }

        // Return a new style instead of changing one shared by other text.
        if (property.equalsIgnoreCase("bold")) {
            return new TextStyle(getOnOff(value), italic, fontName, fontSize, color); // Only change bold.
        }
        else if (property.equalsIgnoreCase("italic")) {
            return new TextStyle(bold, getOnOff(value), fontName, fontSize, color); // Only change italic.
        }
        else if (property.equalsIgnoreCase("font")) {
            return new TextStyle(bold, italic, value, fontSize, color); // Only change the font name.
        }
        else if (property.equalsIgnoreCase("size")) {
            return new TextStyle(bold, italic, fontName, getFontSize(value), color); // Convert the size from text to a number.
        }
        else if (property.equalsIgnoreCase("color")) {
            return new TextStyle(bold, italic, fontName, fontSize, value); // Only change the color.
        }
        else {
            throw new IllegalArgumentException("Unknown formatting option: " + property);
        }
    }

    private static boolean getOnOff(String value) {
        if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
            return true;
        }
        else if (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) {
            return false;
        }
        else {
            throw new IllegalArgumentException("Use on or off for bold and italic");
        }
    }

    private static int getFontSize(String value) {
        try {
            return Integer.parseInt(value); // Check that the size can be read as an integer.
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Font size must be a number");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TextStyle)) {
            return false;
        }

        TextStyle other = (TextStyle) object; // Compare the settings, not the object references.
        return bold == other.bold && italic == other.italic && fontSize == other.fontSize
                && fontName.equals(other.fontName) && color.equals(other.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, italic, fontName, fontSize, color);
    }
}
