package history;

import command.Command;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HistoryManagerTest {

    // Simple object whose value can be modified by commands.
    private static class Counter {
        int value = 0;
    }

    // Test command that adds or subtracts a specified amount.
    private static class AddCommand implements Command {

        private final Counter counter;
        private final int amount;

        AddCommand(Counter counter, int amount) {
            this.counter = counter;
            this.amount = amount;
        }

        @Override
        public void execute() {
            counter.value += amount;
        }

        @Override
        public void undo() {
            counter.value -= amount;
        }
    }

    /**
     * Verifies that multiple commands can be undone and redone
     * in the correct order.
     */
    @Test
    void testUndoAndRedo() {
        HistoryManager history = new HistoryManager();
        Counter counter = new Counter();

        Command first = new AddCommand(counter, 5);
        Command second = new AddCommand(counter, 10);

        // Execute and record two separate commands.
        first.execute();
        history.recordCommand(first);

        second.execute();
        history.recordCommand(second);

        assertEquals(15, counter.value);

        // Undo commands in reverse execution order.
        history.undo();
        assertEquals(5, counter.value);

        history.undo();
        assertEquals(0, counter.value);

        // Redo the commands to restore the original state.
        history.redo();
        assertEquals(5, counter.value);

        history.redo();
        assertEquals(15, counter.value);
    }

    /**
     * Verifies that recording a new command clears redo history.
     */
    @Test
    void testNewCommandClearsRedo() {
        HistoryManager history = new HistoryManager();
        Counter counter = new Counter();

        Command first = new AddCommand(counter, 5);
        Command second = new AddCommand(counter, 10);
        Command third = new AddCommand(counter, 20);

        first.execute();
        history.recordCommand(first);

        second.execute();
        history.recordCommand(second);

        // Undo the latest command to create a redo opportunity.
        history.undo();

        // Execute a new command, invalidating the previous redo history.
        third.execute();
        history.recordCommand(third);

        assertEquals(25, counter.value);

        // Verify that the discarded command cannot be redone.
        assertThrows(IllegalStateException.class, history::redo);
    }

    /**
     * Verifies that empty history rejects undo and redo.
     */
    @Test
    void testEmptyHistory() {
        HistoryManager history = new HistoryManager();

        assertThrows(IllegalStateException.class, history::undo);
        assertThrows(IllegalStateException.class, history::redo);
    }

    /**
     * Verifies that null commands cannot be recorded.
     */
    @Test
    void testNullCommand() {
        HistoryManager history = new HistoryManager();

        assertThrows(NullPointerException.class,
                () -> history.recordCommand(null));
    }
}
