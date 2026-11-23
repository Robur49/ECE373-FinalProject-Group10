
package history;

import command.Command;
import command.CompositeCommand;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MacroManagerTest {

    // Mutable state used to verify command execution.
    private static class Counter {
        int value;
    }

    // Simple reversible command for testing macros.
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
     * Verifies that replay is treated as one undoable operation.
     */
    @Test
    void testRecordReplayAndUndo() {
        MacroManager manager = new MacroManager();
        HistoryManager history = new HistoryManager();
        Counter counter = new Counter();

        manager.startRecording("increments");

        // Simulate edits performed while the macro is recording.
        Command first = new AddCommand(counter, 5);
        first.execute();
        history.recordCommand(first);
        manager.recordCommand(() -> new AddCommand(counter, 5));

        Command second = new AddCommand(counter, 10);
        second.execute();
        history.recordCommand(second);
        manager.recordCommand(() -> new AddCommand(counter, 10));

        manager.stopRecording();

        assertEquals(15, counter.value);

        // Preparing a replay must not execute the commands.
        CompositeCommand replay = manager.replayMacro("increments");
        assertEquals(15, counter.value);

        replay.execute();
        history.recordCommand(replay);
        assertEquals(30, counter.value);

        // One undo reverses the complete replay.
        history.undo();
        assertEquals(15, counter.value);

        history.redo();
        assertEquals(30, counter.value);
    }

    /**
     * Verifies that every replay creates new command instances.
     */
    @Test
    void testFreshCommandsForEachReplay() {
        MacroManager manager = new MacroManager();
        Counter counter = new Counter();
        AtomicInteger created = new AtomicInteger();

        manager.startRecording("increment");

        manager.recordCommand(() -> {
            created.incrementAndGet();
            return new AddCommand(counter, 5);
        });

        manager.stopRecording();

        assertEquals(0, created.get());

        manager.replayMacro("increment");
        assertEquals(1, created.get());

        manager.replayMacro("increment");
        assertEquals(2, created.get());
    }

    /**
     * Verifies that invalid recording operations are rejected.
     */
    @Test
    void testInvalidRecordingOperations() {
        MacroManager manager = new MacroManager();

        assertThrows(IllegalStateException.class,
                manager::stopRecording);

        assertThrows(IllegalArgumentException.class,
                () -> manager.startRecording(" "));

        manager.startRecording("first");

        assertThrows(IllegalStateException.class,
                () -> manager.startRecording("second"));

        assertThrows(IllegalStateException.class,
                () -> manager.replayMacro("first"));

        manager.stopRecording();

        assertThrows(IllegalArgumentException.class,
                () -> manager.replayMacro("missing"));
    }

    /**
     * Verifies that null factories are rejected.
     */
    @Test
    void testNullCommandFactory() {
        MacroManager manager = new MacroManager();

        manager.startRecording("test");

        assertThrows(NullPointerException.class,
                () -> manager.recordCommand(null));
    }
}
