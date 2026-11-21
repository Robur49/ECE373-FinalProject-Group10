package history;

import document.Document;
import document.Paragraph;
import document.Section;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckpointManagerTest {

    /**
     * Verifies that edits after saving do not change the checkpoint.
     */
    @Test
    void testSavedCheckpointIsIndependent() {
        CheckpointManager manager = new CheckpointManager();
        Document document = new Document();

        Section section = document.addSection("Introduction");
        Paragraph paragraph = section.addParagraph("First");
        paragraph.formatRange(0, 5, style -> style.withBold(true));
        section.addSubsection("Details").addParagraph("Nested");

        manager.saveCheckpoint("original", document);

        // Modify the working document after saving.
        paragraph.insertText(0, "Changed ");
        section.getSubsection(0).getParagraph(0).insertText(0, "New ");

        Document restored = manager.restoreCheckpoint("original");

        assertEquals("First",
                restored.getSection(0).getParagraph(0).getText());

        assertEquals("Nested",
                restored.getSection(0).getSubsection(0)
                        .getParagraph(0).getText());

        assertTrue(restored.getSection(0).getParagraph(0)
                .getRuns().get(0).getStyle().isBold());
    }

    /**
     * Verifies that restoring does not expose the stored snapshot.
     */
    @Test
    void testRestoredDocumentIsIndependent() {
        CheckpointManager manager = new CheckpointManager();
        Document document = new Document();

        document.addSection("Introduction").addParagraph("Original");
        manager.saveCheckpoint("draft", document);

        Document firstRestore = manager.restoreCheckpoint("draft");
        firstRestore.getSection(0).getParagraph(0)
                .insertText(0, "Modified ");

        Document secondRestore = manager.restoreCheckpoint("draft");

        assertEquals("Original",
                secondRestore.getSection(0).getParagraph(0).getText());
    }

    /**
     * Verifies that different checkpoint names preserve different states.
     */
    @Test
    void testMultipleCheckpoints() {
        CheckpointManager manager = new CheckpointManager();
        Document document = new Document();

        Paragraph paragraph = document.addSection("Test")
                .addParagraph("First");

        manager.saveCheckpoint("one", document);

        paragraph.insertText(paragraph.length(), " Second");
        manager.saveCheckpoint("two", document);

        assertEquals("First",
                manager.restoreCheckpoint("one")
                        .getSection(0).getParagraph(0).getText());

        assertEquals("First Second",
                manager.restoreCheckpoint("two")
                        .getSection(0).getParagraph(0).getText());
    }

    /**
     * Verifies that invalid checkpoint requests are rejected.
     */
    @Test
    void testInvalidCheckpoints() {
        CheckpointManager manager = new CheckpointManager();
        Document document = new Document();

        assertThrows(IllegalArgumentException.class,
                () -> manager.restoreCheckpoint("missing"));

        assertThrows(IllegalArgumentException.class,
                () -> manager.saveCheckpoint(" ", document));

        assertThrows(NullPointerException.class,
                () -> manager.saveCheckpoint("test", null));
    }
}
