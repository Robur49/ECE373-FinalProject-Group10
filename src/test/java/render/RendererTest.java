//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import model.Document;
import model.Section;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RendererTest {

    @Test
    void testPlainAndMarkupViews() {
        Document document = new Document(); // Make a small document to test rendering.
        document.addSection("root", "Chapter One"); // Create a top-level section.
        Section section = document.section("1");
        section.addParagraph("This is a test paragraph."); // Add content beneath the section title.
        section.title().format(0, 7, style -> style.with("bold", "on")); // Bold only the word "Chapter".

        String plain = new PlainTextRenderer().render(document, 20, new GreedyLineBreaker()); // Display without style tags.
        String markup = new MarkupRenderer().render(document, 20, new GreedyLineBreaker()); // Display with style tags.

        assertTrue(plain.contains("Chapter One"));
        assertFalse(plain.contains("{bold:"));
        assertTrue(markup.contains("{bold:Chapter}"));
        assertTrue(markup.contains("This is a test"));
    }
}
