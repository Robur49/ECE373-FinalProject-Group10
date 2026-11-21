
//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import document.Document;
import document.Paragraph;
import document.Section;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RendererIntegrationTest {

    @Test
    void testPlainAndMarkup() {
        Document document = new Document(); // Create a document to test rendering.
        Section section = document.addSection("Chapter One"); // Add the main section.
        Paragraph paragraph = section.addParagraph("Hello World"); // Add text to the section.

        section.getStyledTitle().formatRange(0, 7, style -> style.withItalic(true)); // Make "Chapter" italic.
        paragraph.formatRange(0, 5, style -> style.withBold(true)); // Make "Hello" bold.

        String plain = new PlainTextRenderer().render(document, 40, new GreedyLineBreaker()); // Render without formatting.
        String markup = new MarkupRenderer().render(document, 40, new GreedyLineBreaker()); // Render with formatting.

        assertTrue(plain.contains("Chapter One")); // Check the section title appears.
        assertFalse(plain.contains("{bold:")); // Plain text should not show formatting.
        assertTrue(markup.contains("{italic:Chapter}")); // Check italic formatting on the title.
        assertTrue(markup.contains("{bold:Hello}")); // Check bold formatting on the paragraph.
        assertEquals("Hello World", paragraph.getText()); // Make sure rendering did not modify the text.
    }

    @Test
    void testNestedSectionAndRunBoundaries() {
        Document document = new Document(); // Create a new document.
        Section section = document.addSection("Main"); // Add the main section.
        Paragraph paragraph = section.addParagraph("abcdef"); // Add a paragraph.

        paragraph.formatRange(0, 2, style -> style.withBold(true)); // Make "ab" bold.
        paragraph.formatRange(2, 4, style -> style.withItalic(true)); // Make "cd" italic.

        section.addSubsection("Sub").addParagraph("Nested text"); // Add a subsection with a paragraph.

        String result = new MarkupRenderer().render(document, 40, new GreedyLineBreaker()); // Render using markup.

        assertTrue(result.contains("{bold:ab}{italic:cd}ef")); // Check that different styles are preserved.
        assertTrue(result.contains("## Sub")); // Check the subsection has the correct heading.
        assertTrue(result.contains("Nested text")); // Check that nested paragraph text appears.
    }

    @Test
    void testRejectBadWidth() {
        Document document = new Document(); // Create an empty document.

        assertThrows(IllegalArgumentException.class,
                () -> new PlainTextRenderer().render(document, 0, new GreedyLineBreaker())); // Reject zero width.

        assertThrows(IllegalArgumentException.class,
                () -> new MarkupRenderer().render(document, -5, new BalancedLineBreaker())); // Reject negative width.
    }
}
