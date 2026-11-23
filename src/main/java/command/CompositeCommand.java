
package command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Groups multiple commands into one undoable operation.
 */
public class CompositeCommand implements Command {

    // Stores commands in their original execution order.
    private final List<Command> commands = new ArrayList<>();

    /**
     * Adds a command to the group.
     *
     * @param command the command to add
     */
    public void addCommand(Command command) {
        commands.add(Objects.requireNonNull(command,
                "Command cannot be null"));
    }

    /**
     * Executes commands in order, rolling back completed
     * operations if a command fails.
     */
    @Override
    public void execute() {
        int completed = 0;

        try {
            for (Command command : commands) {
                command.execute();
                completed++;
            }
        } catch (RuntimeException failure) {
            // Reverse completed commands if execution fails.
            for (int i = completed - 1; i >= 0; i--) {
                try {
                    commands.get(i).undo();
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }

            throw failure;
        }
    }

    /**
     * Reverses commands in the opposite execution order.
     */
    @Override
    public void undo() {
        int completed = 0;

        try {
            for (int i = commands.size() - 1; i >= 0; i--) {
                commands.get(i).undo();
                completed++;
            }
        } catch (RuntimeException failure) {
            // Reapply completed undos if reversal fails.
            for (int i = commands.size() - completed;
                 i < commands.size(); i++) {
                try {
                    commands.get(i).execute();
                } catch (RuntimeException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }

            throw failure;
        }
    }
}
