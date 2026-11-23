package parser;

import document.Document;
import document.Paragraph;
import document.Section;
import document.TextStyle;
import editor.DocumentEditor;
import render.BalancedLineBreaker;
import render.GreedyLineBreaker;
import render.MarkupRenderer;
import render.PlainTextRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Converts one line of user input into operations on the active DocumentEditor.
 * Paths and paragraph indices are zero-based; ranges have an exclusive end.
 * Quoted arguments support spaces, escaped quotes, and escaped backslashes.
 */
public final class CommandParser {
    private final DocumentEditor editor;

    /** Creates a parser for the supplied editor instance. */
    public CommandParser(DocumentEditor editor) {
        this.editor = Objects.requireNonNull(editor, "Editor cannot be null");
    }

    /**
     * Executes one complete input line and returns any response text.
     * Invalid commands throw IllegalArgumentException with a readable message.
     *
     * @param line a command, blank line, or comment beginning with '#'
     * @return a response, or an empty string for commands that need no output
     */
    public String execute(String line) {
        Objects.requireNonNull(line, "Command line cannot be null");
        if (line.isBlank() || line.stripLeading().startsWith("#")) {
            return "";
        }
        List<String> args = tokenize(line);
        if (args.isEmpty()) {
            return "";
        }
        String op = args.get(0).toLowerCase(Locale.ROOT);
        switch (op) {
            case "help":
                expect(args, 1);
                return help();
            case "section":
                expect(args, 2);
                editor.addSection(args.get(1));
                return "Section added.";
            case "subsection":
                expect(args, 3);
                editor.addSubsection(path(args.get(1)), args.get(2));
                return "Subsection added.";
            case "paragraph":
                expect(args, 3);
                editor.addParagraph(path(args.get(1)), args.get(2));
                return "Paragraph added.";
            case "insert":
                expect(args, 5);
                editor.insertText(path(args.get(1)), number(args.get(2)),
                        number(args.get(3)), args.get(4));
                return "Text inserted.";
            case "delete":
                expect(args, 5);
                editor.deleteText(path(args.get(1)), number(args.get(2)),
                        number(args.get(3)), number(args.get(4)));
                return "Text deleted.";
            case "replace":
                expect(args, 6);
                editor.replaceText(path(args.get(1)), number(args.get(2)),
                        number(args.get(3)), number(args.get(4)), args.get(5));
                return "Text replaced.";
            case "move":
                expect(args, 8);
                editor.moveText(path(args.get(1)), number(args.get(2)),
                        number(args.get(3)), number(args.get(4)),
                        path(args.get(5)), number(args.get(6)), number(args.get(7)));
                return "Text moved.";
            case "split":
                expect(args, 4);
                editor.splitParagraph(path(args.get(1)), number(args.get(2)),
                        number(args.get(3)));
                return "Paragraph split.";
            case "merge":
                expect(args, 3);
                editor.mergeParagraph(path(args.get(1)), number(args.get(2)));
                return "Paragraph merged.";
            case "format":
                expect(args, 7);
                editor.formatText(path(args.get(1)), number(args.get(2)),
                        number(args.get(3)), number(args.get(4)),
                        styleOperation(args.get(5), args.get(6)));
                return "Formatting applied.";
            case "format-title":
                expect(args, 6);
                editor.formatTitle(path(args.get(1)), number(args.get(2)),
                        number(args.get(3)), styleOperation(args.get(4), args.get(5)));
                return "Title formatting applied.";
            case "undo":
                expect(args, 1);
                editor.undo();
                return "Undone.";
            case "redo":
                expect(args, 1);
                editor.redo();
                return "Redone.";
            case "checkpoint":
                expect(args, 2);
                editor.saveCheckpoint(args.get(1));
                return "Checkpoint saved.";
            case "restore":
                expect(args, 2);
                editor.restoreCheckpoint(args.get(1));
                return "Checkpoint restored.";
            case "macro-start":
                expect(args, 2);
                editor.startMacroRecording(args.get(1));
                return "Recording macro.";
            case "macro-stop":
                expect(args, 1);
                editor.stopMacroRecording();
                return "Macro saved.";
            case "macro-play":
                expect(args, 2);
                editor.replayMacro(args.get(1));
                return "Macro replayed.";
            case "find":
                if (args.size() != 2 && args.size() != 3) {
                    throw usage("find <text> [whole|substring]");
                }
                boolean wholeWord = false;
                if (args.size() == 3) {
                    String mode = args.get(2).toLowerCase(Locale.ROOT);
                    if (!mode.equals("whole") && !mode.equals("substring")) {
                        throw usage("find <text> [whole|substring]");
                    }
                    wholeWord = mode.equals("whole");
                }
                List<String> results = editor.search(args.get(1), wholeWord);
                return results.isEmpty() ? "No matches." : String.join("\n", results);
            case "replace-first":
                expect(args, 3);
                return editor.replaceFirst(args.get(1), args.get(2))
                        ? "First match replaced." : "No matches.";
            case "replace-all":
                expect(args, 3);
                return editor.replaceAll(args.get(1), args.get(2))
                        ? "All matches replaced." : "No matches.";
            case "renderer":
                expect(args, 2);
                chooseRenderer(args.get(1));
                return "Renderer selected.";
            case "wrap":
                expect(args, 2);
                chooseLineBreaker(args.get(1));
                return "Line-breaking method selected.";
            case "width":
                expect(args, 2);
                editor.setLineWidth(number(args.get(1)));
                return "Line width set.";
            case "show":
                expect(args, 1);
                return editor.render();
            case "stats":
                expect(args, 1);
                return statistics(editor.getDocument());
            default:
                throw new IllegalArgumentException(
                        "Unknown command '" + args.get(0) + "'. Type help for commands.");
        }
    }

    /** Enforces a fixed number of whitespace-separated arguments. */
    private static void expect(List<String> args, int expected) {
        if (args.size() != expected) {
            throw new IllegalArgumentException("Expected " + (expected - 1)
                    + " argument(s) for " + args.get(0) + "; received "
                    + (args.size() - 1) + ". Type help for syntax.");
        }
    }

    /** Parses an unsigned decimal index. */
    private static int number(String text) {
        try {
            int value = Integer.parseInt(text);
            if (value < 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "Expected a nonnegative integer, received: " + text);
        }
    }

    /** Parses paths such as 0, 0.1, and 0.1.2. */
    private static int[] path(String input) {
        String[] parts = input.split("\\.", -1);
        int[] indices = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            indices[i] = number(parts[i]);
        }
        return indices;
    }

    /** Maps a textual formatting choice to an immutable style transformation. */
    private static UnaryOperator<TextStyle> styleOperation(String attribute,
                                                             String value) {
        return switch (attribute.toLowerCase(Locale.ROOT)) {
            case "bold" -> {
                boolean enabled = yesNo(value);
                yield style -> style.withBold(enabled);
            }
            case "italic" -> {
                boolean enabled = yesNo(value);
                yield style -> style.withItalic(enabled);
            }
            case "font" -> style -> style.withFontName(value);
            case "size" -> {
                int size = number(value);
                if (size == 0) {
                    throw new IllegalArgumentException("Font size must be positive");
                }
                yield style -> style.withFontSize(size);
            }
            case "color" -> style -> style.withColor(value);
            default -> throw new IllegalArgumentException(
                    "Unknown style '" + attribute + "'. Use bold, italic, font, size, or color.");
        };
    }

    /** Converts the on/off argument for boolean character attributes. */
    private static boolean yesNo(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true" -> true;
            case "off", "false" -> false;
            default -> throw new IllegalArgumentException(
                    "Expected on or off, received: " + value);
        };
    }

    private void chooseRenderer(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "plain" -> editor.setRenderer(new PlainTextRenderer());
            case "markup" -> editor.setRenderer(new MarkupRenderer());
            default -> throw new IllegalArgumentException("Renderer must be plain or markup");
        }
    }

    private void chooseLineBreaker(String name) {
        switch (name.toLowerCase(Locale.ROOT)) {
            case "greedy" -> editor.setLineBreaker(new GreedyLineBreaker());
            case "balanced" -> editor.setLineBreaker(new BalancedLineBreaker());
            default -> throw new IllegalArgumentException("Wrap method must be greedy or balanced");
        }
    }

    /** Computes status on demand; no editing command maintains status counters. */
    private static String statistics(Document document) {
        int[] counts = new int[2]; // words, characters (excluding separators)
        for (Section section : document.getSections()) {
            countSection(section, counts);
        }
        return "Words: " + counts[0] + ", Characters: " + counts[1]
                + ", Paragraphs: " + document.countParagraphs();
    }

    private static void countSection(Section section, int[] counts) {
        countText(section.getTitle(), counts);
        for (Paragraph paragraph : section.getParagraphs()) {
            countText(paragraph.getText(), counts);
        }
        for (Section child : section.getSubsections()) {
            countSection(child, counts);
        }
    }

    private static void countText(String text, int[] counts) {
        counts[1] += text.length();
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                inWord = false;
            } else if (!inWord) {
                counts[0]++;
                inWord = true;
            }
        }
    }

    /** Error for incorrectly spelled optional syntax. */
    private static IllegalArgumentException usage(String syntax) {
        return new IllegalArgumentException("Usage: " + syntax);
    }

    /** Tokenizes quoted text without treating spaces inside quotes as separators. */
    private static List<String> tokenize(String line) {
        List<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean started = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                quoted = !quoted;
                started = true;
            } else if (character == '\\' && quoted) {
                if (i + 1 == line.length()) {
                    throw new IllegalArgumentException("Unfinished escape in quoted text");
                }
                char next = line.charAt(++i);
                switch (next) {
                    case '"' -> current.append('"');
                    case '\\' -> current.append('\\');
                    case 'n' -> current.append('\n');
                    case 't' -> current.append('\t');
                    default -> throw new IllegalArgumentException(
                            "Unknown escape: \\" + next);
                }
            } else if (!quoted && Character.isWhitespace(character)) {
                if (started) {
                    words.add(current.toString());
                    current.setLength(0);
                    started = false;
                }
            } else {
                current.append(character);
                started = true;
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unterminated quoted argument");
        }
        if (started) {
            words.add(current.toString());
        }
        return words;
    }

    /** Returns the supported commands in a copyable format. */
    public static String help() {
        return String.join("\n",
                "Indices start at 0. Section paths use dots, such as 0.1.",
                "Wrap text containing spaces in double quotes.",
                "section <title> | subsection <parentPath> <title> | paragraph <path> <text>",
                "insert <path> <paragraph> <position> <text>",
                "delete <path> <paragraph> <start> <end>",
                "replace <path> <paragraph> <start> <end> <text>",
                "move <srcPath> <srcParagraph> <start> <end> <destPath> <destParagraph> <position>",
                "split <path> <paragraph> <position> | merge <path> <paragraph>",
                "format <path> <paragraph> <start> <end> <bold|italic|font|size|color> <value>",
                "format-title <path> <start> <end> <bold|italic|font|size|color> <value>",
                "undo | redo | checkpoint <name> | restore <name>",
                "macro-start <name> | macro-stop | macro-play <name>",
                "find <text> [whole|substring] | replace-first <old> <new> | replace-all <old> <new>",
                "renderer <plain|markup> | wrap <greedy|balanced> | width <columns>",
                "show | stats | help | exit");
    }
}
