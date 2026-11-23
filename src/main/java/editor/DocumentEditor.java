package editor;

import command.Command;
import document.Document;
import history.HistoryManager;
import command.RestoreCheckpointCommand;
import history.CheckpointManager;

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

    // Stores named snapshots of the document.
    private final CheckpointManager checkpointManager;

    /**
     * Creates an editor with an empty document and history.
     */
    public DocumentEditor() {
        document = new Document();
        historyManager = new HistoryManager();
        checkpointManager = new CheckpointManager();
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

    /**
     * Saves the current document state under a checkpoint name.
     *
     * @param name the name identifying the checkpoint
     */
    public void saveCheckpoint(String name) {
        // Save an independent snapshot of the active document.
        checkpointManager.saveCheckpoint(name, document);
    }

    /**
     * Restores a named checkpoint as one undoable command.
     *
     * @param name the checkpoint to restore
     */
    public void restoreCheckpoint(String name) {
        RestoreCheckpointCommand command = new RestoreCheckpointCommand(
                this::getDocument,
                this::setActiveDocument,
                checkpointManager,
                name);

        // Record the restoration only after it succeeds.
        executeCommand(command);
    }

    /**
     * Replaces the active document during checkpoint restoration.
     *
     * @param newDocument the document to make active
     */
    private void setActiveDocument(Document newDocument) {
        document = Objects.requireNonNull(
                newDocument, "Active document cannot be null");
    }
}