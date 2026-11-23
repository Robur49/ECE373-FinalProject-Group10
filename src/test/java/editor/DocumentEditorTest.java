
package editor;

import command.Command;
import command.EditCommand;
import document.Document;
import document.Paragraph;
import document.Section;
import render.MarkupRenderer;
import render.BalancedLineBreaker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    /** Checkpoint restoration behaves as one history entry. */
    @Test
    void testCheckpointRestoreUndoRedo() {
        DocumentEditor editor = new DocumentEditor();
        Paragraph original = editor.getDocument()
                .addSection("Title").addParagraph("Hello");
        Document beforeRestore = editor.getDocument();
        editor.saveCheckpoint("initial");
        editor.insertText(new int[]{0}, 0, 5, " world");

        editor.restoreCheckpoint("initial");
        Document restored = editor.getDocument();
        assertNotSame(beforeRestore, restored);
        assertEquals("Hello", restored.getSection(0).getParagraph(0).getText());

        editor.undo();
        assertSame(beforeRestore, editor.getDocument());
        assertEquals("Hello world", original.getText());
        editor.redo();
        assertSame(restored, editor.getDocument());
    }

    /** A nonexistent checkpoint must not appear in undo history. */
    @Test
    void testMissingCheckpointDoesNotEnterHistory() {
        DocumentEditor editor = new DocumentEditor();
        assertThrows(IllegalArgumentException.class,
                () -> editor.restoreCheckpoint("unknown"));
        assertThrows(IllegalStateException.class, editor::undo);
    }

    /** Macro factories retarget the active document after a restore. */
    @Test
    void testMacroReplayAndRestore() {
        DocumentEditor editor = new DocumentEditor();
        editor.getDocument().addSection("Title").addParagraph("A");
        editor.saveCheckpoint("before");

        editor.startMacroRecording("prefix");
        editor.insertText(new int[]{0}, 0, 0, "X");
        editor.stopMacroRecording();
        assertEquals("XA", editor.getDocument().getSection(0).getParagraph(0).getText());

        editor.replayMacro("prefix");
        assertEquals("XXA", editor.getDocument().getSection(0).getParagraph(0).getText());
        editor.undo();
        assertEquals("XA", editor.getDocument().getSection(0).getParagraph(0).getText());

        editor.restoreCheckpoint("before");
        editor.replayMacro("prefix");
        assertEquals("XA", editor.getDocument().getSection(0).getParagraph(0).getText());
        editor.undo();
        assertEquals("A", editor.getDocument().getSection(0).getParagraph(0).getText());
    }

    /** Text editing delegates to reversible commands. */
    @Test
    void testEditingAndFormatting() {
        DocumentEditor editor = new DocumentEditor();
        Paragraph paragraph = editor.getDocument()
                .addSection("Title").addParagraph("abcdef");
        editor.deleteText(new int[]{0}, 0, 1, 3);
        assertEquals("adef", paragraph.getText());
        editor.replaceText(new int[]{0}, 0, 1, 3, "XX");
        assertEquals("aXXf", paragraph.getText());
        editor.formatText(new int[]{0}, 0, 0, 1,
                style -> style.withBold(true));
        assertTrue(paragraph.getRuns().get(0).getStyle().isBold());
        editor.undo();
        assertFalse(paragraph.getRuns().get(0).getStyle().isBold());
    }

    /** Structural operations can be undone. */
    @Test
    void testSplitAndMerge() {
        DocumentEditor editor = new DocumentEditor();
        Section section = editor.getDocument().addSection("Title");
        section.addParagraph("HelloWorld");
        editor.splitParagraph(new int[]{0}, 0, 5);
        assertEquals("Hello", section.getParagraph(0).getText());
        assertEquals("World", section.getParagraph(1).getText());
        editor.mergeParagraph(new int[]{0}, 0);
        assertEquals(1, section.getParagraphs().size());
        assertEquals("HelloWorld", section.getParagraph(0).getText());
        editor.undo();
        assertEquals(2, section.getParagraphs().size());
    }

    /** Move supports two target paragraphs. */
    @Test
    void testMoveText() {
        DocumentEditor editor = new DocumentEditor();
        Section section = editor.getDocument().addSection("Title");
        section.addParagraph("ABCD");
        section.addParagraph("12");
        editor.moveText(new int[]{0}, 0, 1, 3, new int[]{0}, 1, 1);
        assertEquals("AD", section.getParagraph(0).getText());
        assertEquals("1BC2", section.getParagraph(1).getText());
        editor.undo();
        assertEquals("ABCD", section.getParagraph(0).getText());
        assertEquals("12", section.getParagraph(1).getText());
    }

    /** Search and replacement are delegated without bypassing history. */
    @Test
    void testSearchAndReplace() {
        DocumentEditor editor = new DocumentEditor();
        editor.getDocument().addSection("Title").addParagraph("cat cat");
        assertEquals(2, editor.search("cat", false).size());
        assertTrue(editor.replaceFirst("cat", "dog"));
        assertEquals("dog cat", editor.getDocument().getSection(0)
                .getParagraph(0).getText());
        editor.undo();
        assertEquals("cat cat", editor.getDocument().getSection(0)
                .getParagraph(0).getText());
        assertTrue(editor.replaceAll("cat", "dog"));
        assertEquals("dog dog", editor.getDocument().getSection(0)
                .getParagraph(0).getText());
        editor.undo();
        assertEquals("cat cat", editor.getDocument().getSection(0)
                .getParagraph(0).getText());
        assertFalse(editor.replaceFirst("missing", "value"));
    }

    /** Rendering respects the selected renderer and width validation. */
    @Test
    void testRenderingConfiguration() {
        DocumentEditor editor = new DocumentEditor();
        editor.getDocument().addSection("Title").addParagraph("Hello world");
        assertTrue(editor.render().contains("Hello world"));
        editor.setRenderer(new MarkupRenderer());
        editor.setLineBreaker(new BalancedLineBreaker());
        editor.setLineWidth(40);
        assertTrue(editor.render().contains("Hello world"));
        assertThrows(IllegalArgumentException.class,
                () -> editor.setLineWidth(0));
    }

    /** Later macro actions resolve edits after earlier actions have executed. */
    @Test
    void testDependentMacroActions() {
        DocumentEditor editor = new DocumentEditor();
        editor.getDocument().addSection("Title").addParagraph("A");

        editor.startMacroRecording("insertAndReplace");
        editor.insertText(new int[]{0}, 0, 0, "X");
        assertTrue(editor.replaceFirst("X", "Y"));
        editor.stopMacroRecording();
        assertEquals("YA", editor.getDocument().getSection(0)
                .getParagraph(0).getText());

        editor.replayMacro("insertAndReplace");
        assertEquals("YYA", editor.getDocument().getSection(0)
                .getParagraph(0).getText());
        editor.undo();
        assertEquals("YA", editor.getDocument().getSection(0)
                .getParagraph(0).getText());
        editor.redo();
        assertEquals("YYA", editor.getDocument().getSection(0)
                .getParagraph(0).getText());
    }

}
