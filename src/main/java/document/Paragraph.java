package document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Editable paragraph that preserves character formatting. Offsets are zero-based and ranges are [start,end). */
public final class Paragraph {
    private final List<TextRun> runs = new ArrayList<>();
    /** Creates an empty paragraph. */ public Paragraph() { }
    /** Creates a paragraph with default formatting. */ public Paragraph(String text) { insertText(0, text); }
    /** Returns an immutable view of the current text runs. */ public List<TextRun> getRuns() { return Collections.unmodifiableList(runs); }
    /** Returns all text without formatting. */
    public String getText() {
        StringBuilder result = new StringBuilder();
        for (TextRun run : runs) result.append(run.getText());
        return result.toString();
    }
    /** Returns the number of characters. */ public int length() { return getText().length(); }
    private void checkPosition(int position) {
        if (position < 0 || position > length()) throw new IllegalArgumentException("Position outside paragraph");
    }
    private void checkRange(int start, int end) {
        if (start < 0 || end < start || end > length()) throw new IllegalArgumentException("Invalid text range");
    }
    private List<TextStyle> styles() {
        List<TextStyle> values = new ArrayList<>();
        for (TextRun run : runs) for (int i = 0; i < run.length(); i++) values.add(run.getStyle());
        return values;
    }
    private void setContent(String text, List<TextStyle> styles) {
        if (text.length() != styles.size()) throw new IllegalArgumentException("Text/style lengths differ");
        runs.clear();
        if (text.isEmpty()) return;
        StringBuilder current = new StringBuilder();
        TextStyle style = styles.get(0);
        for (int i = 0; i < text.length(); i++) {
            if (!style.equals(styles.get(i))) {
                runs.add(new TextRun(current.toString(), style));
                current.setLength(0);
                style = styles.get(i);
            }
            current.append(text.charAt(i));
        }
        runs.add(new TextRun(current.toString(), style));
    }
    /** Inserts default-styled text at a valid offset. */ public void insertText(int position, String text) {
        insertStyled(position, Objects.requireNonNull(text, "text"), Collections.nCopies(text.length(), TextStyle.DEFAULT));
    }
    public void insertStyled(int position, String text, List<TextStyle> incoming) {
        checkPosition(position);
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(incoming, "incoming");
        if (text.length() != incoming.size() || incoming.contains(null)) throw new IllegalArgumentException("Invalid styles");
        List<TextStyle> updated = styles();
        updated.addAll(position, incoming);
        String original = getText();
        setContent(original.substring(0, position) + text + original.substring(position), updated);
    }
    /** Removes a range of characters. */ public void deleteText(int start, int end) {
        checkRange(start, end);
        List<TextStyle> updated = styles();
        updated.subList(start, end).clear();
        String original = getText();
        setContent(original.substring(0, start) + original.substring(end), updated);
    }
    /** Replaces a range with default-styled text. */ public void replaceText(int start, int end, String replacement) {
        Objects.requireNonNull(replacement, "replacement");
        checkRange(start, end);
        deleteText(start, end);
        insertText(start, replacement);
    }
    /** Changes the formatting of a selected range without changing its characters. */
    public void formatRange(int start, int end, UnaryOperator<TextStyle> operation) {
        checkRange(start, end);
        Objects.requireNonNull(operation, "operation");
        List<TextStyle> updated = styles();
        for (int i = start; i < end; i++) updated.set(i, Objects.requireNonNull(operation.apply(updated.get(i)), "style"));
        setContent(getText(), updated);
    }
    /** Returns an independent copy of this paragraph. */ public Paragraph copy() {
        Paragraph result = new Paragraph();
        result.insertStyled(0, getText(), styles());
        return result;
    }
    /** Splits this paragraph, retaining the left side and returning the right side. */
    public Paragraph split(int position) {
        checkPosition(position);
        String original = getText();
        List<TextStyle> values = styles();
        Paragraph right = new Paragraph();
        right.insertStyled(0, original.substring(position), new ArrayList<>(values.subList(position, values.size())));
        setContent(original.substring(0, position), new ArrayList<>(values.subList(0, position)));
        return right;
    }
    /** Appends another paragraph's characters and formatting. */ public void append(Paragraph other) {
        Objects.requireNonNull(other, "other");
        insertStyled(length(), other.getText(), other.styles());
    }
    public List<TextStyle> stylesInRange(int start, int end) {
        checkRange(start, end);
        return new ArrayList<>(styles().subList(start, end));
    }
}
