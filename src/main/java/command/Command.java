package command;


/**
 * Defines a document-changing operation that can be executed
 * and reversed.
 */
public interface Command {

    /**
     * Performs the requested document modification.
     */
    void execute();

    /**
     * Reverses a previously completed modification.
     */
    void undo();
}
