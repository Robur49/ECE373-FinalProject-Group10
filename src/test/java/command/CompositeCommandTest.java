
package command;

import history.HistoryManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositeCommandTest {

    // Simple mutable state for testing command operations.
    private static class Counter {
        int value;
    }

    // Reversible command that changes the counter.
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
     * Verifies execution, undo, and repeated execution.
     */
    @Test
    void testExecuteAndUndo() {
        Counter counter = new Counter();
        CompositeCommand composite = new CompositeCommand();

        composite.addCommand(new AddCommand(counter, 5));
        composite.addCommand(new AddCommand(counter, 10));

        composite.execute();
        assertEquals(15, counter.value);

        composite.undo();
        assertEquals(0, counter.value);

        composite.execute();
        assertEquals(15, counter.value);
    }

    /**
     * Verifies that HistoryManager treats the group as one command.
     */
    @Test
    void testSingleHistoryEntry() {
        Counter counter = new Counter();
        HistoryManager history = new HistoryManager();
        CompositeCommand composite = new CompositeCommand();

        composite.addCommand(new AddCommand(counter, 5));
        composite.addCommand(new AddCommand(counter, 10));

        composite.execute();
        history.recordCommand(composite);

        history.undo();
        assertEquals(0, counter.value);

        history.redo();
        assertEquals(15, counter.value);
    }

    /**
     * Verifies that successful commands are reversed after failure.
     */
    @Test
    void testExecutionFailureRollback() {
        Counter counter = new Counter();
        CompositeCommand composite = new CompositeCommand();

        composite.addCommand(new AddCommand(counter, 5));

        composite.addCommand(new Command() {
            @Override
            public void execute() {
                throw new IllegalStateException("Execution failed.");
            }

            @Override
            public void undo() {
                // Nothing to reverse for this failed operation.
            }
        });

        assertThrows(IllegalStateException.class,
                composite::execute);

        assertEquals(0, counter.value);
    }

    /**
     * Verifies that a failed undo restores earlier undone work.
     */
    @Test
    void testUndoFailureRollback() {
        Counter counter = new Counter();
        CompositeCommand composite = new CompositeCommand();

        composite.addCommand(new Command() {
            @Override
            public void execute() {
                counter.value += 5;
            }

            @Override
            public void undo() {
                throw new IllegalStateException("Undo failed.");
            }
        });

        composite.addCommand(new AddCommand(counter, 10));

        composite.execute();
        assertEquals(15, counter.value);

        assertThrows(IllegalStateException.class,
                composite::undo);

        assertEquals(15, counter.value);
    }

    /**
     * Verifies that null commands cannot be added.
     */
    @Test
    void testNullCommand() {
        CompositeCommand composite = new CompositeCommand();

        assertThrows(NullPointerException.class,
                () -> composite.addCommand(null));
    }
}
