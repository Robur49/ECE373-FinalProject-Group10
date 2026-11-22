package command;

import document.Paragraph;
import document.Section;
import history.HistoryManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that EditCommand restores text, formatting, and paragraph identity
 * using the project's existing document model and HistoryManager.
 */
class EditCommandTest {

    /**
     * Checks that two recorded edits are undone in reverse order and
     * reapplied in their original order.
     */
    @Test
    void testInsertAndReplace() {
        Paragraph paragraph = new Paragraph("Hello world");
        HistoryManager history = new HistoryManager();

        // Execute and record two edits as separate history entries.
        Command insert = EditCommand.insert(paragraph, 5, ",");
        insert.execute();
        history.recordCommand(insert);

        Command replace = EditCommand.replace(paragraph, 7, 12, "team");
        replace.execute();
        history.recordCommand(replace);

        assertEquals("Hello, team", paragraph.getText());

        // Each undo should remove only the latest edit.
        history.undo();
        assertEquals("Hello, world", paragraph.getText());
        history.undo();
        assertEquals("Hello world", paragraph.getText());

        // Redo both operations to reproduce the final text.
        history.redo();
        history.redo();
        assertEquals("Hello, team", paragraph.getText());
    }

    /**
     * Verifies that undo restores the deleted text with its original bold
     * formatting, and that the deletion can subsequently be redone.
     */
    @Test
    void testDeletePreservesFormatting() {
        Paragraph paragraph = new Paragraph("Bold normal");
        // Make the deleted word bold to test style preservation.
        paragraph.formatRange(0, 4, style -> style.withBold(true));

        Command delete = EditCommand.delete(paragraph, 0, 5);
        delete.execute();
        assertEquals("normal", paragraph.getText());

        delete.undo();
        assertEquals("Bold normal", paragraph.getText());
        assertTrue(paragraph.getRuns().get(0).getStyle().isBold());

        delete.execute();
        assertEquals("normal", paragraph.getText());
    }

    /**
     * Checks that moving styled text changes both paragraphs and that undo
     * restores their original text and formatting.
     */
    @Test
    void testMoveBetweenParagraphs() {
        Paragraph source = new Paragraph("abcdef");
        Paragraph destination = new Paragraph("123");
        // Move an italicized substring into another paragraph.
        source.formatRange(2, 5, style -> style.withItalic(true));

        Command move = EditCommand.move(source, 2, 5, destination, 1);
        move.execute();
        assertEquals("abf", source.getText());
        assertEquals("1cde23", destination.getText());
        assertTrue(destination.getRuns().get(1).getStyle().isItalic());

        move.undo();
        assertEquals("abcdef", source.getText());
        assertEquals("123", destination.getText());
        assertTrue(source.getRuns().get(1).getStyle().isItalic());
    }

    /**
     * Verifies that an in-place move can be reversed without needing a
     * second paragraph.
     */
    @Test
    void testMoveWithinParagraph() {
        Paragraph paragraph = new Paragraph("abcdef");
        Command move = EditCommand.move(paragraph, 0, 3, paragraph, 6);

        move.execute();
        assertEquals("defabc", paragraph.getText());
        move.undo();
        assertEquals("abcdef", paragraph.getText());
    }

    /**
     * Checks that a split can be undone and redone while retaining the same
     * paragraph objects, not just equivalent text.
     */
    @Test
    void testSplitUndoRedo() {
        Section section = new Section("Title");
        Paragraph original = section.addParagraph("abcdef");
        Command split = EditCommand.split(section, 0, 3);

        // Retain the new paragraph reference to check identity after redo.
        split.execute();
        Paragraph right = section.getParagraph(1);
        assertEquals("abc", original.getText());
        assertEquals("def", right.getText());

        split.undo();
        assertEquals(1, section.getParagraphs().size());
        assertEquals("abcdef", original.getText());

        // Redo must restore the same right-hand paragraph object.
        split.execute();
        assertEquals(2, section.getParagraphs().size());
        assertSame(right, section.getParagraph(1));
    }

    /**
     * Verifies that merging adjacent paragraphs is reversible and that undo
     * restores the second paragraph's original object identity.
     */
    @Test
    void testMergeUndoRedo() {
        Section section = new Section("Title");
        Paragraph first = section.addParagraph("one");
        Paragraph second = section.addParagraph("two");
        Command merge = EditCommand.merge(section, 0);

        merge.execute();
        assertEquals("onetwo", first.getText());
        assertEquals(1, section.getParagraphs().size());

        // The removed paragraph must reappear as the original object.
        merge.undo();
        assertSame(second, section.getParagraph(1));
        assertEquals("one", first.getText());
        assertEquals("two", second.getText());

        merge.execute();
        assertEquals("onetwo", first.getText());
    }

    /**
     * Verifies that a styled section title returns to its prior font size
     * after undoing a formatting command.
     */
    @Test
    void testFormatTitle() {
        Section section = new Section("Title");
        Command format = EditCommand.format(section.getStyledTitle(),
                0, 5, style -> style.withFontSize(18));

        format.execute();
        assertEquals(18, section.getStyledTitle().getRuns().get(0)
                .getStyle().getFontSize());

        format.undo();
        assertEquals(12, section.getStyledTitle().getRuns().get(0)
                .getStyle().getFontSize());
    }

    /**
     * Ensures an invalid insertion does not change the paragraph and that
     * a command which never succeeded cannot be undone.
     */
    @Test
    void testInvalidEditDoesNotModifyDocument() {
        Paragraph paragraph = new Paragraph("Hello");
        Command invalid = EditCommand.insert(paragraph, 99, "!");

        assertThrows(IllegalArgumentException.class, invalid::execute);
        assertEquals("Hello", paragraph.getText());
        assertThrows(IllegalStateException.class, invalid::undo);
    }
}
