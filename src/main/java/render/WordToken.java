//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

// Stores one word and its original location in the text.
public class WordToken {

    private final String text; // The word (or piece of a long word) being displayed.
    private final int sourceStart; // Original character index used to look up formatting.
    private final boolean breakBefore; // True when this piece must begin on a new line.

    public WordToken(String text, int sourceStart, boolean breakBefore) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Word cannot be empty");
        }
        if (sourceStart < 0) {
            throw new IllegalArgumentException("Word position cannot be negative");
        }

        this.text = text; // Keep the characters to display.
        this.sourceStart = sourceStart; // Remember the location in the original paragraph.
        this.breakBefore = breakBefore; // Keep whether wrapping must happen before this piece.
    }

    public String text() {
        return text;
    }

    public int sourceStart() {
        return sourceStart; // Used to find the original character formatting.
    }

    public boolean breakBefore() {
        return breakBefore; // Tells the line breaker to start a new line if needed.
    }
}
