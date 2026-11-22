package command;

import document.Paragraph;
import document.Section;
import document.TextStyle;
import editing.DocumentEditingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Wraps document-editing operations as commands that can be undone and redone.
 *
 * <p>On the first execution, the command saves the affected state before and
 * after the edit. Undo restores the original state, while redo restores the
 * saved result without running the editing operation a second time.
 */
public final class EditCommand implements Command {

    // Shared editing service used for the first execution of each command.
    private static final DocumentEditingService EDITOR = new DocumentEditingService();

    // The requested edit, saved for its first execution.
    private final Runnable operation;

    // Paragraph edits save individual paragraphs; structural edits save a section.
    private final List<Paragraph> affectedParagraphs;
    private final Section affectedSection;

    // Snapshots retained so undo and redo can restore exact document states.
    private State before;
    private State after;
    private boolean executed;

    /**
     * Creates a command with the editing action and the state it must track.
     *
     * @param operation action to perform on first execution
     * @param paragraphs paragraphs affected by a text or formatting edit
     * @param section section affected by a structural paragraph edit, or null
     */
    private EditCommand(Runnable operation, List<Paragraph> paragraphs,
                        Section section) {
        this.operation = Objects.requireNonNull(operation, "operation");
        this.affectedParagraphs = List.copyOf(paragraphs);
        this.affectedSection = section;
    }

    /**
     * Prepares a command to insert text at a position in a paragraph.
     *
     * @param paragraph paragraph to edit
     * @param position insertion index
     * @param text text to insert
     * @return a command that can insert, undo, and redo the change
     */
    public static EditCommand insert(Paragraph paragraph, int position, String text) {
        Objects.requireNonNull(paragraph, "paragraph");
        Objects.requireNonNull(text, "text");
        return new EditCommand(() -> EDITOR.insert(paragraph, position, text),
                List.of(paragraph), null);
    }

    /**
     * Prepares a command to delete a range of text, including its formatting.
     *
     * @param paragraph paragraph to edit
     * @param start beginning of the range (inclusive)
     * @param end end of the range (exclusive)
     * @return a reversible deletion command
     */
    public static EditCommand delete(Paragraph paragraph, int start, int end) {
        Objects.requireNonNull(paragraph, "paragraph");
        return new EditCommand(() -> EDITOR.delete(paragraph, start, end),
                List.of(paragraph), null);
    }

    /**
     * Prepares a command to replace a text range with new text.
     *
     * @param paragraph paragraph to edit
     * @param start beginning of the range (inclusive)
     * @param end end of the range (exclusive)
     * @param text replacement text
     * @return a reversible replacement command
     */
    public static EditCommand replace(Paragraph paragraph, int start, int end,
                                      String text) {
        Objects.requireNonNull(paragraph, "paragraph");
        Objects.requireNonNull(text, "text");
        return new EditCommand(() -> EDITOR.replace(paragraph, start, end, text),
                List.of(paragraph), null);
    }

    /**
     * Prepares a command to move a text range within or between paragraphs.
     * Both paragraphs are saved when the source and destination differ.
     *
     * @param source paragraph containing the text to move
     * @param start beginning of the source range (inclusive)
     * @param end end of the source range (exclusive)
     * @param destination paragraph receiving the moved text
     * @param destinationPosition insertion index in the destination
     * @return a reversible move command
     */
    public static EditCommand move(Paragraph source, int start, int end,
                                   Paragraph destination, int destinationPosition) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        // Avoid saving the same paragraph twice for a within-paragraph move.
        List<Paragraph> paragraphs = source == destination
                ? List.of(source) : List.of(source, destination);
        return new EditCommand(() -> EDITOR.move(source, start, end,
                destination, destinationPosition), paragraphs, null);
    }

    /**
     * Prepares a command to split one paragraph into two in a section.
     * A section snapshot records paragraph order and object references.
     *
     * @param section section containing the paragraph
     * @param paragraphIndex index of the paragraph to split
     * @param position position at which to split the paragraph
     * @return a reversible split command
     */
    public static EditCommand split(Section section, int paragraphIndex, int position) {
        Objects.requireNonNull(section, "section");
        return new EditCommand(() -> EDITOR.split(section, paragraphIndex, position),
                List.of(), section);
    }

    /**
     * Prepares a command to merge a paragraph with the one immediately after it.
     *
     * @param section section containing both paragraphs
     * @param paragraphIndex index of the first paragraph
     * @return a reversible merge command
     */
    public static EditCommand merge(Section section, int paragraphIndex) {
        Objects.requireNonNull(section, "section");
        return new EditCommand(() -> EDITOR.merge(section, paragraphIndex),
                List.of(), section);
    }

    /**
     * Prepares a command to change the formatting of a text range.
     * This also works with a section title represented as a styled paragraph.
     *
     * @param paragraph paragraph or styled title to format
     * @param start beginning of the range (inclusive)
     * @param end end of the range (exclusive)
     * @param operation transformation applied to each affected text style
     * @return a reversible formatting command
     */
    public static EditCommand format(Paragraph paragraph, int start, int end,
                                     UnaryOperator<TextStyle> operation) {
        Objects.requireNonNull(paragraph, "paragraph");
        Objects.requireNonNull(operation, "operation");
        return new EditCommand(() -> paragraph.formatRange(start, end, operation),
                List.of(paragraph), null);
    }

    /**
     * Performs the edit once and records its before/after snapshots.
     * Later executions restore the saved result when redoing the command.
     */
    @Override
    public void execute() {
        if (executed) {
            throw new IllegalStateException("Command is already executed.");
        }
        // Redo uses the saved result rather than repeating the original edit.
        if (after != null) {
            restoreSafely(after, before);
            executed = true;
            return;
        }

        // Capture the original state before allowing the edit to modify it.
        State startingState = captureState();
        try {
            operation.run();
            State resultingState = captureState();
            before = startingState;
            after = resultingState;
            executed = true;
        } catch (RuntimeException failure) {
            // Attempt to restore the starting state after a failed edit.
            try {
                startingState.restore();
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    /**
     * Restores the state from before the edit while retaining the result
     * snapshot for a possible redo.
     */
    @Override
    public void undo() {
        if (!executed) {
            throw new IllegalStateException("Command has not been executed.");
        }
        restoreSafely(before, after);
        executed = false;
    }

    /**
     * Captures paragraphs for text edits or the entire paragraph list for
     * structural split/merge operations.
     *
     * @return a snapshot capable of restoring the affected objects
     */
    private State captureState() {
        return affectedSection == null
                ? new ParagraphState(affectedParagraphs)
                : new SectionState(affectedSection);
    }

    /**
     * Tries to restore one snapshot and returns to the other if that fails.
     * The original exception is preserved, including rollback failures.
     *
     * @param desired state to restore
     * @param rollback state to attempt if restoration fails
     */
    private static void restoreSafely(State desired, State rollback) {
        try {
            desired.restore();
        } catch (RuntimeException failure) {
            try {
                rollback.restore();
            } catch (RuntimeException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    /**
     * Replaces a paragraph's content using a snapshot without replacing
     * the original paragraph object referenced by other commands.
     *
     * @param target existing paragraph object to update
     * @param snapshot saved paragraph content and formatting
     */
    private static void restoreParagraph(Paragraph target, Paragraph snapshot) {
        target.deleteText(0, target.length());
        target.append(snapshot);
    }

    /** A saved editing state that knows how to restore its original targets. */
    private interface State {
        void restore();
    }

    /** Saves paragraph contents and styles for text or formatting edits. */
    private static final class ParagraphState implements State {
        private final List<Paragraph> targets;
        private final List<Paragraph> snapshots = new ArrayList<>();

        private ParagraphState(List<Paragraph> paragraphs) {
            targets = List.copyOf(paragraphs);
            for (Paragraph paragraph : targets) {
                snapshots.add(paragraph.copy());
            }
        }

        @Override
        public void restore() {
            for (int i = 0; i < targets.size(); i++) {
                restoreParagraph(targets.get(i), snapshots.get(i));
            }
        }
    }

    /** Saves paragraph order, identities, contents, and styles for split/merge. */
    private static final class SectionState implements State {
        private final Section section;
        private final List<Paragraph> targets;
        private final List<Paragraph> snapshots = new ArrayList<>();

        private SectionState(Section section) {
            this.section = section;
            targets = new ArrayList<>(section.getParagraphs());
            for (Paragraph paragraph : targets) {
                snapshots.add(paragraph.copy());
            }
        }

        @Override
        public void restore() {
            // Rebuild the list with the original paragraph objects. Commands
            // created before this edit may still hold references to them.
            while (!section.getParagraphs().isEmpty()) {
                section.removeParagraph(section.getParagraphs().size() - 1);
            }
            for (int i = 0; i < targets.size(); i++) {
                section.insertParagraph(i, targets.get(i));
                restoreParagraph(targets.get(i), snapshots.get(i));
            }
        }
    }
}
