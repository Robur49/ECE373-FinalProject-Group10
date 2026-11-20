//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package model;

// TEMPORARY CLASS: gives the renderer a paragraph with formatted text.
public class Paragraph {
    private final StyledText text;

    public Paragraph(String value) {
        text = new StyledText(value); // Store text and formatting together.
    }

    public StyledText text() {
        return text; // Allow the renderer and formatting commands to access this text.
    }
}
