//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import document.TextStyle;

// Shows the document without any formatting labels.
public final class PlainTextRenderer extends AbstractRenderer {

    @Override
    public String name() {
        return "plain"; // Name used when selecting the plain-text view.
    }

    @Override
    protected String titlePrefix(int depth) {
        return "  ".repeat(depth - 1); // Indent titles but do not add formatting symbols.
    }

    @Override
    protected String encode(String text, TextStyle style) {
        return text; // Show the original characters without bold or color tags.
    }
}
