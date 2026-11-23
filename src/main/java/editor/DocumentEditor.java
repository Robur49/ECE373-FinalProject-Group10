package editor;

import command.Command;
import document.Document;
import history.HistoryManager;

import java.util.Objects;

/**
 * Coordinates document operations and command history.
 *
 * Additional services will be integrated as their
 * corresponding components become available.
 */
public class DocumentEditor {

    // The document currently being edited.
    private Document document;

    // Records completed commands and manages undo/redo.
    private final HistoryManager historyManager;

    /**
     * Creates an editor with an empty document and history.
     */
    public DocumentEditor() {
        document = new Document();
        historyManager = new HistoryManager();
    }

    /**
     * Returns the currently active document.
     *
     * @return the active document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Executes a command and records it if successful.
     *
     * @param command the document-changing command
     */
    public void executeCommand(Command command) {
        Objects.requireNonNull(command, "Command cannot be null");

        // Only record commands that complete successfully.
        command.execute();
        historyManager.recordCommand(command);
    }

    /**
     * Reverses the most recently completed command.
     */
    public void undo() {
        historyManager.undo();
    }

    /**
     * Reapplies the most recently undone command.
     */
    public void redo() {
        historyManager.redo();
    }
}