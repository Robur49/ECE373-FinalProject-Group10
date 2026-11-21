
package history;

import command.Command;
import command.CompositeCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Records named command sequences and prepares them for replay.
 * Each recorded factory creates a fresh command for each replay.
 */
public class MacroManager {

    // Stores the command factories associated with each macro name.
    private final Map<String, List<Supplier<Command>>> macros =
            new HashMap<>();

    private boolean isRecording;
    private String recordingName;
    private List<Supplier<Command>> recording;

    /**
     * Begins recording commands under the specified name.
     *
     * @param name the macro name
     */
    public void startRecording(String name) {
        Objects.requireNonNull(name, "Macro name cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Macro name cannot be blank.");
        }

        if (isRecording) {
            throw new IllegalStateException(
                    "A macro is already being recorded.");
        }

        recordingName = name;
        recording = new ArrayList<>();
        isRecording = true;
    }

    /**
     * Records a way to recreate a successfully executed command.
     *
     * @param commandFactory creates a new command for replay
     */
    public void recordCommand(Supplier<Command> commandFactory) {
        Objects.requireNonNull(commandFactory,
                "Command factory cannot be null");

        if (isRecording) {
            recording.add(commandFactory);
        }
    }

    /**
     * Stops recording and saves the current macro.
     */
    public void stopRecording() {
        if (!isRecording) {
            throw new IllegalStateException(
                    "No macro is being recorded.");
        }

        // Preserve the completed sequence under its macro name.
        macros.put(recordingName, new ArrayList<>(recording));

        recordingName = null;
        recording = null;
        isRecording = false;
    }

    /**
     * Builds a new composite command from a saved macro.
     *
     * @param name the macro to replay
     * @return the composite command containing the recorded operations
     */
    public CompositeCommand replayMacro(String name) {
        Objects.requireNonNull(name, "Macro name cannot be null");

        if (isRecording) {
            throw new IllegalStateException(
                    "Stop recording before replaying a macro.");
        }

        List<Supplier<Command>> saved = macros.get(name);

        if (saved == null) {
            throw new IllegalArgumentException(
                    "Macro not found: " + name);
        }

        CompositeCommand composite = new CompositeCommand();

        // Create fresh commands instead of reusing executed objects.
        for (Supplier<Command> commandFactory : saved) {
            composite.addCommand(Objects.requireNonNull(
                    commandFactory.get(),
                    "Command factory returned null"));
        }

        return composite;
    }
}
