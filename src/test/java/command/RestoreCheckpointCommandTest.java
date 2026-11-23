
package command;

import document.Document;
import document.Paragraph;
import history.CheckpointManager;
import history.HistoryManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestoreCheckpointCommandTest {


    /**
     * Simple reversible editing command for testing document history.
     */
    private static class AppendTextCommand implements Command {

        private final Paragraph paragraph;
        private final String text;

        /**
         * Creates a command that appends text to a paragraph.
         *
         * @param paragraph the paragraph to modify
         * @param text the text to append
         */
        AppendTextCommand(Paragraph paragraph, String text) {
            this.paragraph = paragraph;
            this.text = text;
        }

        /**
         * Appends the specified text to the paragraph.
         */
        @Override
        public void execute() {
            paragraph.insertText(paragraph.length(), text);
        }

        /**
         * Removes the text previously appended by the command.
         */
        @Override
        public void undo() {
            int end = paragraph.length();

            // Remove the appended text from the end of the paragraph.
            paragraph.deleteText(end - text.length(), end);
        }
    }


    /**
     * Verifies restoration, undo, and redo through HistoryManager.
     */
    @Test
    void testRestoreUndoRedo() {
        CheckpointManager checkpoints = new CheckpointManager();
        HistoryManager history = new HistoryManager();

        Document original = new Document();
        Paragraph paragraph = original.addSection("Introduction")
                .addParagraph("Original");

        checkpoints.saveCheckpoint("draft", original);

        // Modify the document after saving its checkpoint.
        paragraph.insertText(paragraph.length(), " edited");

        // Simulate the active document reference owned by DocumentEditor.
        AtomicReference<Document> active = new AtomicReference<>(original);

        RestoreCheckpointCommand restore = new RestoreCheckpointCommand(
                active::get, active::set, checkpoints, "draft");

        restore.execute();
        history.recordCommand(restore);

        Document restored = active.get();

        assertEquals("Original",
                restored.getSection(0).getParagraph(0).getText());

        // Undo returns to the previously edited document.
        history.undo();

        assertSame(original, active.get());
        assertEquals("Original edited",
                active.get().getSection(0).getParagraph(0).getText());

        // Redo returns to the same restored document instance.
        history.redo();

        assertSame(restored, active.get());
        assertEquals("Original",
                active.get().getSection(0).getParagraph(0).getText());
    }

    /**
     * Verifies that a missing checkpoint leaves the document unchanged.
     */
    @Test
    void testMissingCheckpoint() {
        CheckpointManager checkpoints = new CheckpointManager();
        Document original = new Document();

        AtomicReference<Document> active = new AtomicReference<>(original);

        RestoreCheckpointCommand restore = new RestoreCheckpointCommand(
                active::get, active::set, checkpoints, "missing");

        assertThrows(IllegalArgumentException.class, restore::execute);
        assertSame(original, active.get());
    }

    /**
     * Verifies that edits made before and after checkpoint restoration
     * can be undone and redone in the correct order.
     */
    @Test
    void testEditingAcrossCheckpointRestoration() {
        CheckpointManager checkpoints = new CheckpointManager();
        HistoryManager history = new HistoryManager();

        Document original = new Document();
        Paragraph paragraph = original.addSection("Test")
                .addParagraph("Original");

        // Simulate the active document managed by DocumentEditor.
        AtomicReference<Document> active = new AtomicReference<>(original);

        // Save the initial document state.
        checkpoints.saveCheckpoint("draft", original);

        // Edit the original document.
        Command firstEdit = new AppendTextCommand(paragraph, " edited");
        firstEdit.execute();
        history.recordCommand(firstEdit);

        // Restore the saved checkpoint.
        Command restore = new RestoreCheckpointCommand(
                active::get, active::set, checkpoints, "draft");

        restore.execute();
        history.recordCommand(restore);

        Document restored = active.get();

        // Edit the restored document.
        Paragraph restoredParagraph = restored.getSection(0).getParagraph(0);
        Command secondEdit = new AppendTextCommand(
                restoredParagraph, " revised");

        secondEdit.execute();
        history.recordCommand(secondEdit);

        assertEquals("Original revised", restoredParagraph.getText());

        // Undo both the latest edit and checkpoint restoration.
        history.undo();
        assertEquals("Original", restoredParagraph.getText());

        history.undo();
        assertSame(original, active.get());
        assertEquals("Original edited", paragraph.getText());

        // Undo the edit made before restoration.
        history.undo();
        assertEquals("Original", paragraph.getText());

        // Redo all three operations in their original order.
        history.redo();
        assertEquals("Original edited", paragraph.getText());

        history.redo();
        assertSame(restored, active.get());

        history.redo();
        assertEquals("Original revised", restoredParagraph.getText());
    }

}
