package editor;

import Search.SearchService;
import command.Command;
import command.EditCommand;
import command.RestoreCheckpointCommand;
import document.Document;
import document.Paragraph;
import document.Section;
import document.TextStyle;
import history.CheckpointManager;
import history.HistoryManager;
import history.MacroManager;
import render.GreedyLineBreaker;
import render.LineBreakingStrategy;
import render.PlainTextRenderer;
import render.Renderer;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Coordinates document edits, history, checkpoints, macros, search and rendering.
 * Domain objects and services retain responsibility for the actual operations.
 */
public class DocumentEditor {

    // Checkpoint restoration can replace the entire active document.
    private Document document;
    private final HistoryManager historyManager;
    private final CheckpointManager checkpointManager;
    private final MacroManager macroManager;
    private final SearchService searchService;

    private Renderer renderer;
    private LineBreakingStrategy lineBreaker;
    private int lineWidth;

    /** Creates an empty editor with a plain-text, 80-column display. */
    public DocumentEditor() {
        document = new Document();
        historyManager = new HistoryManager();
        checkpointManager = new CheckpointManager();
        macroManager = new MacroManager();
        searchService = new SearchService();
        renderer = new PlainTextRenderer();
        lineBreaker = new GreedyLineBreaker();
        lineWidth = 80;
    }

    /** Returns the active document, which may change after checkpoint restoration. */
    public Document getDocument() {
        return document;
    }

    /**
     * Executes a command and records it for undo/redo only on success.
     * For macro recording, use executeAndRecord or an indexed editing method.
     *
     * @param command command to execute
     */
    public void executeCommand(Command command) {
        Objects.requireNonNull(command, "Command cannot be null");
        command.execute();
        historyManager.recordCommand(command);
    }

    /**
     * Executes a freshly created command and records its factory if a macro
     * is active. Factories should resolve the active document when invoked.
     *
     * @param factory creates a new command for each use
     */
    public void executeAndRecord(Supplier<Command> factory) {
        Objects.requireNonNull(factory, "Command factory cannot be null");
        Command command = Objects.requireNonNull(factory.get(),
                "Command factory returned null");
        executeCommand(command);
        recordMacroFactory(factory);
    }

    /**
     * Defers command creation until its place in a macro actually executes.
     * This matters when a later recorded edit depends on an earlier edit.
     * The created command is retained so undo/redo use its own snapshots.
     */
    private static Command deferred(Supplier<Command> factory) {
        return new Command() {
            private Command delegate;

            @Override
            public void execute() {
                if (delegate == null) {
                    Command fresh = Objects.requireNonNull(factory.get(),
                            "Macro command factory returned null");
                    fresh.execute();
                    delegate = fresh;
                } else {
                    delegate.execute();
                }
            }

            @Override
            public void undo() {
                if (delegate == null) {
                    throw new IllegalStateException(
                            "Macro command has not executed");
                }
                delegate.undo();
            }
        };
    }

    /** Adds a factory only while a macro is being recorded. */
    private void recordMacroFactory(Supplier<Command> factory) {
        macroManager.recordCommand(() -> deferred(factory));
    }

    /** Undoes the most recent recorded document change. */
    public void undo() {
        historyManager.undo();
    }

    /** Redoes the most recently undone document change. */
    public void redo() {
        historyManager.redo();
    }

    /** Saves an independent snapshot under a name. */
    public void saveCheckpoint(String name) {
        checkpointManager.saveCheckpoint(name, document);
    }

    /** Restores a named snapshot as one undoable command. */
    public void restoreCheckpoint(String name) {
        executeCommand(new RestoreCheckpointCommand(
                this::getDocument, this::setActiveDocument,
                checkpointManager, name));
    }

    /** Changes the document reference for restore/undo/redo. */
    private void setActiveDocument(Document replacement) {
        document = Objects.requireNonNull(replacement,
                "Active document cannot be null");
    }

    /** Starts recording successful factory-backed editing actions. */
    public void startMacroRecording(String name) {
        macroManager.startRecording(name);
    }

    /** Saves the currently recorded macro. */
    public void stopMacroRecording() {
        macroManager.stopRecording();
    }

    /** Replays a named macro as one undoable group. */
    public void replayMacro(String name) {
        executeCommand(macroManager.replayMacro(name));
    }

    /**
     * Retrieves a paragraph using an index path into the active document.
     * The first path element is a top-level section; later elements select
     * nested subsections. Resolving it on demand supports checkpoint restore.
     */
    private Paragraph paragraph(int[] path, int paragraphIndex) {
        return document.findSection(path).getParagraph(paragraphIndex);
    }

    /** Copies a section path so later caller changes cannot alter macro replay. */
    private static int[] pathCopy(int[] path) {
        return Objects.requireNonNull(path, "Section path cannot be null").clone();
    }

    /** Creates a top-level section as an undoable, macro-recordable action. */
    public void addSection(String title) {
        requireTitle(title);
        executeAndRecord(() -> new Command() {
            private Document target;
            private Section created;
            private int index;

            @Override
            public void execute() {
                if (created == null) {
                    target = document;
                    created = new Section(title);
                    index = target.getSections().size();
                }
                target.insertSection(index, created);
            }

            @Override
            public void undo() {
                target.removeSection(index);
            }
        });
    }

    /** Creates a nested section under the specified parent section. */
    public void addSubsection(int[] parentPath, String title) {
        requireTitle(title);
        int[] path = pathCopy(parentPath);
        executeAndRecord(() -> new Command() {
            private Section parent;
            private Section created;
            private int index;

            @Override
            public void execute() {
                if (created == null) {
                    parent = document.findSection(path);
                    created = new Section(title);
                    index = parent.getSubsections().size();
                }
                parent.insertSubsection(index, created);
            }

            @Override
            public void undo() {
                parent.removeSubsection(index);
            }
        });
    }

    /** Appends a paragraph to the specified section, recording the change. */
    public void addParagraph(int[] sectionPath, String text) {
        Objects.requireNonNull(text, "Paragraph text cannot be null");
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> new Command() {
            private Section parent;
            private Paragraph created;
            private int index;

            @Override
            public void execute() {
                if (created == null) {
                    parent = document.findSection(path);
                    created = new Paragraph(text);
                    index = parent.getParagraphs().size();
                }
                parent.insertParagraph(index, created);
            }

            @Override
            public void undo() {
                parent.removeParagraph(index);
            }
        });
    }

    /** Validates a user-supplied section title. */
    private static void requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Section title cannot be blank");
        }
    }

    /** Formats a section title using the existing undoable formatting command. */
    public void formatTitle(int[] sectionPath, int start, int end,
                            UnaryOperator<TextStyle> operation) {
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> EditCommand.format(
                document.findSection(path).getStyledTitle(), start, end, operation));
    }

    /** Inserts text as one undoable, macro-recordable action. */
    public void insertText(int[] sectionPath, int paragraphIndex,
                           int position, String text) {
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> EditCommand.insert(
                paragraph(path, paragraphIndex), position, text));
    }

    /** Deletes text in the half-open range [start, end). */
    public void deleteText(int[] sectionPath, int paragraphIndex,
                           int start, int end) {
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> EditCommand.delete(
                paragraph(path, paragraphIndex), start, end));
    }

    /** Replaces text in the half-open range [start, end). */
    public void replaceText(int[] sectionPath, int paragraphIndex,
                            int start, int end, String text) {
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> EditCommand.replace(
                paragraph(path, paragraphIndex), start, end, text));
    }

    /** Moves text within or between paragraphs as one command. */
    public void moveText(int[] sourcePath, int sourceParagraph,
                         int start, int end, int[] destinationPath,
                         int destinationParagraph, int destinationPosition) {
        int[] source = pathCopy(sourcePath);
        int[] destination = pathCopy(destinationPath);
        executeAndRecord(() -> EditCommand.move(
                paragraph(source, sourceParagraph), start, end,
                paragraph(destination, destinationParagraph), destinationPosition));
    }

    /** Splits a paragraph at a character offset. */
    public void splitParagraph(int[] sectionPath, int paragraphIndex,
                               int position) {
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> EditCommand.split(
                document.findSection(path), paragraphIndex, position));
    }

    /** Merges a paragraph with the one immediately after it. */
    public void mergeParagraph(int[] sectionPath, int paragraphIndex) {
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> EditCommand.merge(
                document.findSection(path), paragraphIndex));
    }

    /** Formats a text range using a style transformation. */
    public void formatText(int[] sectionPath, int paragraphIndex,
                           int start, int end,
                           UnaryOperator<TextStyle> operation) {
        int[] path = pathCopy(sectionPath);
        executeAndRecord(() -> EditCommand.format(
                paragraph(path, paragraphIndex), start, end, operation));
    }

    /** Finds text in section titles and body paragraphs. */
    public List<String> search(String query, boolean wholeWord) {
        return searchService.Find(document, query, wholeWord);
    }

    /**
     * Replaces the first matching occurrence, when present.
     *
     * @return true when a replacement was performed
     */
    public boolean replaceFirst(String target, String replacement) {
        EditCommand command = searchService.replaceFirst(
                document, target, replacement);
        if (command == null) {
            return false;
        }
        executeCommand(command);
        recordMacroFactory(() -> searchService.replaceFirst(
                document, target, replacement));
        return true;
    }

    /**
     * Replaces all matching occurrences as one undoable operation.
     *
     * @return true when at least one replacement was performed
     */
    public boolean replaceAll(String target, String replacement) {
        // Validation is delegated to SearchService before deciding on no-op.
        command.CompositeCommand command = searchService.replaceAll(
                document, target, replacement);
        if (search(target, false).isEmpty()) {
            return false;
        }
        executeCommand(command);
        recordMacroFactory(() -> searchService.replaceAll(
                document, target, replacement));
        return true;
    }

    /** Selects the document renderer. */
    public void setRenderer(Renderer selectedRenderer) {
        renderer = Objects.requireNonNull(selectedRenderer,
                "Renderer cannot be null");
    }

    /** Selects the line-breaking strategy. */
    public void setLineBreaker(LineBreakingStrategy strategy) {
        lineBreaker = Objects.requireNonNull(strategy,
                "Line breaker cannot be null");
    }

    /** Sets a positive display width. */
    public void setLineWidth(int width) {
        LineBreakingStrategy.validateWidth(width);
        lineWidth = width;
    }

    /** Renders the active document using the selected display settings. */
    public String render() {
        return renderer.render(document, lineWidth, lineBreaker);
    }
}
