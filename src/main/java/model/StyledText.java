//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

// Stores the text as separate pieces so different sections can have different styles.
public final class StyledText {

    private final List<TextRun> runs = new ArrayList<>();

    public StyledText() {
    }

    public StyledText(String text) {
        Objects.requireNonNull(text, "text");
        addRun(text, TextStyle.DEFAULT); // Start with one unformatted run; later edits can split it.
    }

    public StyledText copy() {
        StyledText copy = new StyledText();
        copy.appendAll(this); // Copy each text run without sharing a modifiable list.
        return copy;
    }

    public void restore(StyledText saved) {
        Objects.requireNonNull(saved, "saved");
        List<TextRun> savedRuns = List.copyOf(saved.runs); // Copy before changing this object.
        runs.clear();
        runs.addAll(savedRuns); // Restore both the characters and their previous styles.
    }

    public List<TextRun> runs() {
        return List.copyOf(runs); // Stop other classes from adding or removing runs directly.
    }

    public int length() {
        int total = 0;
        for (TextRun run : runs) {
            total += run.text().length(); // Count the characters in each run.
        }
        return total;
    }

    public String plainText() {
        StringBuilder text = new StringBuilder();
        for (TextRun run : runs) {
            text.append(run.text()); // Put the words together without changing their stored styles.
        }
        return text.toString();
    }

    public TextStyle styleAt(int index) {
        if (index < 0 || index >= length()) {
            throw new IllegalArgumentException("Character index out of bounds: " + index);
        }

        int position = 0;
        for (TextRun run : runs) {
            int end = position + run.text().length(); // End position of the run in the full text.
            if (index < end) {
                return run.style(); // The requested character belongs to this run.
            }
            position = end;
        }
        throw new IllegalStateException("Could not find style at index");
    }

    public void validatePosition(int position) {
        if (position < 0 || position > length()) {
            throw new IllegalArgumentException("Position " + position + " outside 0.." + length());
        }
    }

    public void validateRange(int start, int end) {
        if (start < 0 || end < start || end > length()) {
            throw new IllegalArgumentException("Range [" + start + ", " + end + ") outside 0.." + length());
        }
    }

    public StyledText slice(int start, int end) {
        validateRange(start, end);
        StyledText selected = new StyledText();
        int position = 0;

        for (TextRun run : runs) {
            int runEnd = position + run.text().length(); // Find this run's end in the original text.
            int left = Math.max(start, position); // Start of the overlap with the selected range.
            int right = Math.min(end, runEnd); // End of the overlap with the selected range.

            if (left < right) {
                String part = run.text().substring(left - position, right - position); // Convert full-text positions to local run positions.
                selected.addRun(part, run.style()); // Keep the original formatting of this piece.
            }
            position = runEnd;
        }
        return selected;
    }

    public void insert(int position, String text) {
        Objects.requireNonNull(text, "text");
        validatePosition(position);
        if (text.isEmpty()) {
            return;
        }

        TextStyle style = getInsertStyle(position); // New characters inherit the style at their insertion point.
        StyledText addedText = new StyledText();
        addedText.addRun(text, style);
        insertStyled(position, addedText);
    }

    public void insertStyled(int position, StyledText addedText) {
        validatePosition(position);
        Objects.requireNonNull(addedText, "addedText");

        StyledText result = slice(0, position); // Keep the original characters before the insertion.
        result.appendAll(addedText); // Put the new text between the left and right pieces.
        result.appendAll(slice(position, length())); // Keep the characters after the insertion.
        setRuns(result);
    }

    public void delete(int start, int end) {
        validateRange(start, end);

        StyledText result = slice(0, start); // Keep the text before the selected range.
        result.appendAll(slice(end, length())); // Skip the deleted range but keep the text after it.
        setRuns(result);
    }

    public void replace(int start, int end, String replacement) {
        Objects.requireNonNull(replacement, "replacement");
        validateRange(start, end);

        TextStyle style = getInsertStyle(start);
        StyledText result = slice(0, start);
        result.addRun(replacement, style); // Put the replacement where the old selection was.
        result.appendAll(slice(end, length()));
        setRuns(result);
    }

    public void format(int start, int end, UnaryOperator<TextStyle> change) {
        validateRange(start, end);
        Objects.requireNonNull(change, "change");

        StyledText result = new StyledText();
        int position = 0;

        for (TextRun run : runs) {
            int runEnd = position + run.text().length();
            int left = Math.max(start, position);
            int right = Math.min(end, runEnd);

            if (left >= right) {
                result.addRun(run.text(), run.style()); // Preserve the style of each appended piece. // Leave runs outside the range unchanged.
            }
            else {
                // Split the run into before, selected, and after sections.
                result.addRun(run.text().substring(0, left - position), run.style()); // Keep the unselected part before the range.
                result.addRun(run.text().substring(left - position, right - position), change.apply(run.style())); // Format only the selected characters.
                result.addRun(run.text().substring(right - position), run.style()); // Keep the unselected part after the range.
            }
            position = runEnd;
        }
        setRuns(result);
    }

    public void appendAll(StyledText other) {
        Objects.requireNonNull(other, "other");
        for (TextRun run : List.copyOf(other.runs)) {
            addRun(run.text(), run.style());
        }
    }

    private TextStyle getInsertStyle(int position) {
        if (runs.isEmpty()) {
            return TextStyle.DEFAULT; // Empty text has no neighboring style to copy.
        }
        if (position < length()) {
            return styleAt(position); // Inserted text matches the following character's style.
        }
        return styleAt(length() - 1); // At the end, use the style of the last character.
    }

    private void addRun(String text, TextStyle style) {
        if (text.isEmpty()) {
            return;
        }

        int last = runs.size() - 1;
        if (last >= 0 && runs.get(last).style().equals(style)) {
            TextRun previous = runs.get(last); // The new text has the same style as the previous run.
            runs.set(last, new TextRun(previous.text() + text, style)); // Combine matching styles.
        }
        else {
            runs.add(new TextRun(text, style)); // Start a new run when the formatting is different.
        }
    }

    private void setRuns(StyledText updated) {
        runs.clear();
        runs.addAll(updated.runs); // Replace the old run layout with the edited version.
    }
}
