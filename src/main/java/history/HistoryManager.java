package history;

import command.Command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Manages the undo and redo history of document-changing commands.
 */
public class HistoryManager {

    // Stores executed commands, with the most recent on top.
    private final Deque<Command> undoStack = new ArrayDeque<>();

    // Stores undone commands that can still be redone.
    private final Deque<Command> redoStack = new ArrayDeque<>();

    /**
     * Records a command that has already executed successfully.
     *
     * @param command the completed command to record
     */
    public void recordCommand(Command command) {
        // Reject invalid commands before modifying history.
        Objects.requireNonNull(command, "Command cannot be null");

        undoStack.push(command);

        // A new edit invalidates the previous redo history.
        redoStack.clear();
    }

    /**
     * Reverses the most recently completed command.
     */
    public void undo() {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to undo.");
        }

        // Keep the command in history until undo succeeds.
        Command command = undoStack.peek();
        command.undo();

        // Move the reversed command to the redo stack.
        undoStack.pop();
        redoStack.push(command);
    }

    /**
     * Reapplies the most recently undone command.
     */
    public void redo() {


        if (redoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to redo.");
        }

        // Re-execute without clearing the remaining redo history.
        Command command = redoStack.peek();
        command.execute();

        // Return the completed command to the undo stack.
        redoStack.pop();
        undoStack.push(command);
    }
}
