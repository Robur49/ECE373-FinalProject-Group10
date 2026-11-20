package editing;

import org.junit.jupiter.api.Test;
import document.Document;
import document.Section;
import document.Paragraph;
import document.TextStyle;
import document.TextRun;
import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for the independently developed document model. */
class DocumentModelTest {
    @Test void nestedSectionsAndCopy() {
        Document document = new Document();
        Section intro = document.addSection("Introduction");
        intro.addParagraph("First");
        intro.addSubsection("Methods").addSubsection("Setup").addParagraph("Second");
        assertEquals(2, document.countParagraphs());
        assertEquals("Second", document.findSection(0, 0, 0).getParagraph(0).getText());
        Document copy = document.copy();
        copy.getSection(0).getParagraph(0).insertText(0, "New ");
        assertEquals("First", document.getSection(0).getParagraph(0).getText());
    }
    @Test void editingAndValidation() {
        Paragraph paragraph = new Paragraph("abcdef");
        paragraph.insertText(3, "XY");
        assertEquals("abcXYdef", paragraph.getText());
        paragraph.deleteText(3, 5);
        paragraph.replaceText(1, 3, "Q");
        assertEquals("aQdef", paragraph.getText());
        assertThrows(IllegalArgumentException.class, () -> paragraph.deleteText(5, 2));
        assertEquals("aQdef", paragraph.getText());
    }
    @Test void moveUsesOriginalOffsetsAndPreservesStyle() {
        DocumentEditingService editor = new DocumentEditingService();
        Paragraph paragraph = new Paragraph("abcdef");
        paragraph.formatRange(1, 3, s -> s.withBold(true));
        editor.move(paragraph, 1, 3, paragraph, 6);
        assertEquals("adefbc", paragraph.getText());
        assertTrue(paragraph.getRuns().get(1).getStyle().isBold());
        assertThrows(IllegalArgumentException.class, () -> editor.move(paragraph, 1, 4, paragraph, 2));
    }
    @Test void moveAcrossParagraphsAndSplitMerge() {
        DocumentEditingService editor = new DocumentEditingService();
        Section section = new Section("Title");
        Paragraph first = section.addParagraph("Hello world");
        Paragraph second = section.addParagraph("again");
        first.formatRange(6, 11, s -> s.withItalic(true));
        editor.move(first, 6, 11, second, 0);
        assertEquals("Hello ", first.getText());
        assertEquals("worldagain", second.getText());
        assertTrue(second.getRuns().get(0).getStyle().isItalic());
        editor.split(section, 1, 5);
        assertEquals(3, section.getParagraphs().size());
        editor.merge(section, 1);
        assertEquals("worldagain", section.getParagraph(1).getText());
        assertEquals(2, section.getParagraphs().size());
    }
    @Test void formattingPreservesCharactersAndSupportsTitle() {
        Paragraph paragraph = new Paragraph("one two three");
        paragraph.formatRange(0, 3, s -> s.withBold(true));
        paragraph.formatRange(2, 7, s -> s.withItalic(true));
        assertEquals("one two three", paragraph.getText());
        assertEquals(4, paragraph.getRuns().size());
        Section section = new Section("Heading");
        section.formatTitle(0, 7, s -> s.withFontSize(18));
        assertEquals(18, section.getStyledTitle().getRuns().get(0).getStyle().getFontSize());
    }
    @Test void emptyAndBoundaryOperations() {
        DocumentEditingService editor = new DocumentEditingService();
        Section section = new Section("Empty");
        Paragraph paragraph = section.addParagraph("");
        editor.insert(paragraph, 0, "A");
        editor.split(section, 0, 0);
        assertEquals("", section.getParagraph(0).getText());
        assertEquals("A", section.getParagraph(1).getText());
        editor.merge(section, 0);
        assertEquals("A", section.getParagraph(0).getText());
        assertThrows(IllegalArgumentException.class, () -> editor.merge(section, 0));
    }
}
