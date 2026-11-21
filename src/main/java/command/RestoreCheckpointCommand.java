
package command;

import document.Document;
import history.CheckpointManager;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Restores a saved checkpoint as one undoable operation.
 */
public class RestoreCheckpointCommand implements Command {

    private final Supplier<Document> getDocument;
    private final Consumer<Document> setDocument;
    private final CheckpointManager checkpointManager;
    private final String checkpointName;

    private Document previousDocument;
    private Document restoredDocument;
    private boolean executed;

    /**
     * Creates a command for restoring a named checkpoint.
     *
     * @param getDocument retrieves the active document
     * @param setDocument replaces the active document
     * @param checkpointManager manages saved checkpoints
     * @param checkpointName the checkpoint to restore
     */
    public RestoreCheckpointCommand(
            Supplier<Document> getDocument,
            Consumer<Document> setDocument,
            CheckpointManager checkpointManager,
            String checkpointName) {

        this.getDocument = Objects.requireNonNull(getDocument);
        this.setDocument = Objects.requireNonNull(setDocument);
        this.checkpointManager = Objects.requireNonNull(checkpointManager);
        this.checkpointName = Objects.requireNonNull(checkpointName);
    }

    /**
     * Restores the checkpoint, preserving the previous document.
     */
    @Override
    public void execute() {
        if (executed) {
            throw new IllegalStateException(
                    "Checkpoint restoration is already active.");
        }

        if (restoredDocument == null) {
            // Save both document references for future undo and redo.
            Document previous = Objects.requireNonNull(
                    getDocument.get(), "Active document cannot be null");

            Document restored =
                    checkpointManager.restoreCheckpoint(checkpointName);

            setDocument.accept(restored);

            previousDocument = previous;
            restoredDocument = restored;
        } else {
            // Redo must reuse the original restored document object.
            if (getDocument.get() != previousDocument) {
                throw new IllegalStateException(
                        "Unexpected document during redo.");
            }

            setDocument.accept(restoredDocument);
        }

        executed = true;
    }

    /**
     * Returns to the document active before restoration.
     */
    @Override
    public void undo() {
        if (!executed) {
            throw new IllegalStateException(
                    "Checkpoint restoration is not active.");
        }

        if (getDocument.get() != restoredDocument) {
            throw new IllegalStateException(
                    "Unexpected document during undo.");
        }

        // Restore the previous document without losing its edits.
        setDocument.accept(previousDocument);
        executed = false;
    }
}
