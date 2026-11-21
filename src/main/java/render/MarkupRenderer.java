//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import document.TextStyle;
import java.util.ArrayList;
import java.util.List;

// Makes bold, italic, font, size, and color visible in the console.
public final class MarkupRenderer extends AbstractRenderer {

    @Override
    public String name() {
        return "markup"; // Name used to select the marked-up view.
    }

    @Override
    protected String titlePrefix(int depth) {
        return "  ".repeat(depth - 1) + "#".repeat(depth) + " "; // Mark section levels.
    }

    @Override
    protected String encode(String text, TextStyle style) {
        if (style.equals(TextStyle.DEFAULT)) {
            return text; // No formatting needed if the style is default.
        }

        List<String> labels = new ArrayList<>(); // Store the formatting labels.

        if (style.isBold()) {
            labels.add("bold"); // Check if the text is bold.
        }

        if (style.isItalic()) {
            labels.add("italic"); // Check if the text is italic.
        }

        if (!style.getFontName().equals(TextStyle.DEFAULT.getFontName())) {
            labels.add("font=" + style.getFontName()); // Show the custom font.
        }

        if (style.getFontSize() != TextStyle.DEFAULT.getFontSize()) {
            labels.add("size=" + style.getFontSize()); // Show the custom font size.
        }

        if (!style.getColor().equals(TextStyle.DEFAULT.getColor())) {
            labels.add("color=" + style.getColor()); // Show the custom text color.
        }

        return "{" + String.join(",", labels) + ":" + text + "}"; // Example: {bold:Hello}.
    }
}
