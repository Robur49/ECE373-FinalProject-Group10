//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import model.TextStyle;
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
            return text; // Leave text alone when it uses the default style.
        }

        List<String> labels = new ArrayList<>(); // Collect only the styles that are different from the default.

        if (style.bold()) {
            labels.add("bold"); // Show that this run is bold.
        }
        if (style.italic()) {
            labels.add("italic"); // Show that this run is italic.
        }
        if (!style.fontName().equals(TextStyle.DEFAULT.fontName())) {
            labels.add("font=" + style.fontName()); // Show the custom font name.
        }
        if (style.fontSize() != TextStyle.DEFAULT.fontSize()) {
            labels.add("size=" + style.fontSize()); // Show the custom font size.
        }
        if (!style.color().equals(TextStyle.DEFAULT.color())) {
            labels.add("color=" + style.color()); // Show the custom color.
        }

        return "{" + String.join(",", labels) + ":" + text + "}"; // Example: {bold,italic:Hello}.
    }
}
