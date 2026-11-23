package history;

import document.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Manages named snapshots of document states.
 */
public class CheckpointManager {

    // Stores an independent document snapshot for each checkpoint name.
    private final Map<String, Document> checkpoints = new HashMap<>();

    /**
     * Saves a copy of the document under the provided name.
     *
     * @param name the name identifying the checkpoint
     * @param document the document to save
     */
    public void saveCheckpoint(String name, Document document) {
        Objects.requireNonNull(name, "Checkpoint name cannot be null");
        Objects.requireNonNull(document, "Document cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Checkpoint name cannot be blank.");
        }

        // Save a copy so future edits cannot change the checkpoint.
        checkpoints.put(name, document.copy());
    }

    /**
     * Returns an independent copy of a saved checkpoint.
     *
     * @param name the name of the checkpoint to restore
     * @return a copy of the saved document
     */
    public Document restoreCheckpoint(String name) {
        Objects.requireNonNull(name, "Checkpoint name cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Checkpoint name cannot be blank.");
        }

        Document snapshot = checkpoints.get(name);

        if (snapshot == null) {
            throw new IllegalArgumentException("Checkpoint not found: " + name);
        }

        // Protect the saved checkpoint from changes to the restored document.
        return snapshot.copy();
    }
}

