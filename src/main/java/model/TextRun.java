//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package model;

import java.util.Objects;

// Stores text that all uses the same formatting.
public class TextRun {

    private final String text; // Characters stored in this one run.
    private final TextStyle style; // Style shared by every character in this run.

    public TextRun(String text, TextStyle style) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
        if (style == null) {
            throw new IllegalArgumentException("Text needs a style");
        }

        this.text = text; // Store the text after checking that it is not empty.
        this.style = style; // Keep the style with its matching text.
    }

    public String text() {
        return text; // Give the caller the characters in this run.
    }

    public TextStyle style() {
        return style; // Give the caller this run's formatting.
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TextRun)) {
            return false;
        }

        TextRun other = (TextRun) object;
        return text.equals(other.text) && style.equals(other.style); // Both text and style must match.
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, style);
    }
}
