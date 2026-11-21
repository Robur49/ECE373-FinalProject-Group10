//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import document.Document;
import document.Section;
import document.Paragraph;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RendererTest {

    @Test
    void testPlainAndMarkupViews() {
        Document document = new Document(); // Create a document for testing.
        Section section = document.addSection("Chapter One"); // Add a section.
        Paragraph paragraph = section.addParagraph("Hello World"); // Add a paragraph.

        section.formatTitle(0, 7, style -> style.withBold(true)); // Make "Chapter" bold.
        paragraph.formatRange(0, 5, style -> style.withItalic(true)); // Make "Hello" italic.

        String plain = new PlainTextRenderer().render(document, 40, new GreedyLineBreaker());
        String markup = new MarkupRenderer().render(document, 40, new GreedyLineBreaker());

        assertTrue(plain.contains("Chapter One")); // Check the title is displayed.
        assertTrue(plain.contains("Hello World")); // Check paragraph is displayed.
        assertFalse(plain.contains("{bold:")); // Plain view should hide formatting.
        assertTrue(markup.contains("{bold:Chapter}")); // Check bold formatting.
        assertTrue(markup.contains("{italic:Hello}")); // Check italic formatting.
    }

    @Test
    void testNestedSections() {
        Document document = new Document();
        Section main = document.addSection("Main Section"); // Create the first section.
        main.addParagraph("First paragraph");

        Section sub = main.addSubsection("Subsection"); // Add a subsection.
        sub.addParagraph("Nested paragraph"); // Add text inside the subsection.

        String result = new MarkupRenderer().render(document, 40, new GreedyLineBreaker());

        assertTrue(result.contains("# Main Section")); // Check main section.
        assertTrue(result.contains("## Subsection")); // Check subsection.
        assertTrue(result.contains("Nested paragraph")); // Check nested text.
    }

    @Test
    void testMultipleFormattingStyles() {
        Document document = new Document();
        Section section = document.addSection("Test");
        Paragraph paragraph = section.addParagraph("HelloWorld");

        paragraph.formatRange(0, 5, style -> style.withBold(true)); // Bold "Hello".
        paragraph.formatRange(5, 10, style -> style.withItalic(true)); // Italicize "World".

        String result = new MarkupRenderer().render(document, 40, new GreedyLineBreaker());

        assertTrue(result.contains("{bold:Hello}")); // Check bold portion.
        assertTrue(result.contains("{italic:World}")); // Check italic portion.
        assertEquals("HelloWorld", paragraph.getText()); // Formatting should not change text.
    }

    @Test
    void testLineBreaking() {
        Document document = new Document();
        Section section = document.addSection("Test");
        section.addParagraph("a bb c");

        String greedy = new PlainTextRenderer().render(document, 5, new GreedyLineBreaker());
        String balanced = new PlainTextRenderer().render(document, 5, new BalancedLineBreaker());

        assertTrue(greedy.contains("a bb")); // Greedy fits both words on one line.
        assertTrue(balanced.contains("bb c")); // Balanced uses a different word grouping.
        assertNotEquals(greedy, balanced); // Both algorithms should produce different output.
    }

    @Test
    void testInvalidWidth() {
        Document document = new Document();
        document.addSection("Test");

        assertThrows(IllegalArgumentException.class,
                () -> new PlainTextRenderer().render(document, 0, new GreedyLineBreaker())); // Width cannot be zero.

        assertThrows(IllegalArgumentException.class,
                () -> new MarkupRenderer().render(document, -5, new BalancedLineBreaker())); // Width cannot be negative.
    }

    @Test
    void testRenderingDoesNotChangeDocument() {
        Document document = new Document();
        Section section = document.addSection("Test");
        Paragraph paragraph = section.addParagraph("Original Text");

        String before = paragraph.getText(); // Save the original text.

        new PlainTextRenderer().render(document, 20, new GreedyLineBreaker());
        new MarkupRenderer().render(document, 20, new BalancedLineBreaker());

        assertEquals(before, paragraph.getText()); // Rendering should not modify anything.
    }
}
