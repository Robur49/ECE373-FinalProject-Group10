
package editor;

import command.Command;
import command.EditCommand;
import document.Document;
import document.Paragraph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that DocumentEditor coordinates document changes
 * and command history correctly.
 */
class DocumentEditorTest {

    /**
     * Verifies that a new editor begins with an empty document.
     */
    @Test
    void testInitialDocument() {
        DocumentEditor editor = new DocumentEditor();

        assertNotNull(editor.getDocument());
        assertTrue(editor.getDocument().getSections().isEmpty());
    }

    /**
     * Verifies that an editing command can be executed,
     * undone, and redone through DocumentEditor.
     */
    @Test
    void testExecuteUndoRedo() {
        DocumentEditor editor = new DocumentEditor();

        Paragraph paragraph = editor.getDocument()
                .addSection("Introduction")
                .addParagraph("Hello");

        // Prepare a reversible text insertion.
        Command insert = EditCommand.insert(paragraph, 5, " world");

        // Execute the command and verify the resulting text.
        editor.executeCommand(insert);
        assertEquals("Hello world", paragraph.getText());

        // Undo should restore the original paragraph.
        editor.undo();
        assertEquals("Hello", paragraph.getText());

        // Redo should restore the completed edit.
        editor.redo();
        assertEquals("Hello world", paragraph.getText());
    }

    /**
     * Verifies that multiple editing commands are undone
     * in reverse order and redone in their original order.
     */
    @Test
    void testMultipleCommands() {
        DocumentEditor editor = new DocumentEditor();

        Paragraph paragraph = editor.getDocument()
                .addSection("Test")
                .addParagraph("A");

        // Perform two separate document changes.
        editor.executeCommand(EditCommand.insert(paragraph, 1, "B"));
        editor.executeCommand(EditCommand.insert(paragraph, 2, "C"));

        assertEquals("ABC", paragraph.getText());

        // Undo the most recent command first.
        editor.undo();
        assertEquals("AB", paragraph.getText());

        editor.undo();
        assertEquals("A", paragraph.getText());

        // Redo both commands in their original order.
        editor.redo();
        editor.redo();

        assertEquals("ABC", paragraph.getText());
    }

    /**
     * Verifies that a failed command is not recorded
     * in the editor's undo history.
     */
    @Test
    void testFailedCommandNotRecorded() {
        DocumentEditor editor = new DocumentEditor();

        // This command intentionally fails without changing the document.
        Command failingCommand = new Command() {

            @Override
            public void execute() {
                throw new IllegalStateException("Execution failed.");
            }

            @Override
            public void undo() {
                // No changes were made, so there is nothing to undo.
            }
        };

        // The editor should propagate the execution failure.
        assertThrows(IllegalStateException.class,
                () -> editor.executeCommand(failingCommand));

        // No successful command should have entered history.
        assertThrows(IllegalStateException.class, editor::undo);
    }

    /**
     * Verifies that null commands are rejected.
     */
    @Test
    void testNullCommand() {
        DocumentEditor editor = new DocumentEditor();

        assertThrows(NullPointerException.class,
                () -> editor.executeCommand(null));
    }
}
