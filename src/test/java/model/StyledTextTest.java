//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StyledTextTest {

    @Test
    void testFormattingAcrossDifferentStyles() {
        StyledText text = new StyledText("Hello world"); // Start with one unformatted paragraph.

        text.format(0, 5, style -> style.with("bold", "on")); // Format "Hello" as bold.
        text.format(3, 8, style -> style.with("color", "red")); // Color a range that overlaps the bold text.

        assertEquals("Hello world", text.plainText()); // Formatting should not change the words.
        assertTrue(text.styleAt(3).bold()); // This character should have both formatting changes.
        assertEquals("red", text.styleAt(3).color());
        assertFalse(text.styleAt(7).bold());
        assertEquals("red", text.styleAt(7).color());
    }

    @Test
    void testRestoreFormatting() {
        StyledText text = new StyledText("Hello world");
        StyledText saved = text.copy(); // Save the original before making a change.

        text.format(0, 5, style -> style.with("italic", "on"));
        assertTrue(text.styleAt(2).italic());

        text.restore(saved); // Restore the original text and formatting.
        assertFalse(text.styleAt(2).italic()); // Restoring should also restore the original style.
        assertEquals("Hello world", text.plainText());
    }

    @Test
    void testInvalidRangeDoesNotChangeText() {
        StyledText text = new StyledText("Example");

        assertThrows(IllegalArgumentException.class, // Formatting beyond the end of the text should fail.
                () -> text.format(0, 20, style -> style.with("bold", "on")));
        assertEquals("Example", text.plainText());
        assertFalse(text.styleAt(0).bold());
    }
}
