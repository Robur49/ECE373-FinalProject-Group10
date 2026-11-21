package editing;

import java.util.Objects;
import document.Paragraph;
import document.Section;

/** Coordinates text editing without depending on command parsing, rendering, or history. */
public final class DocumentEditingService {
    /** Creates a stateless editing service. */ public DocumentEditingService() { }
    /** Inserts text into a paragraph. */ public void insert(Paragraph paragraph, int position, String text) {
        Objects.requireNonNull(paragraph).insertText(position, text);
    }
    /** Deletes text from a paragraph. */ public void delete(Paragraph paragraph, int start, int end) {
        Objects.requireNonNull(paragraph).deleteText(start, end);
    }
    /** Replaces a paragraph's selected range. */ public void replace(Paragraph paragraph, int start, int end, String text) {
        Objects.requireNonNull(paragraph).replaceText(start, end, text);
    }
    /** Moves a range to another paragraph or to a position in the same paragraph.
     * All positions are interpreted before the move; destination inside the selected
     * source range is rejected to avoid ambiguous moves.
     */
    public void move(Paragraph source, int start, int end, Paragraph destination, int destinationPosition) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        if (start < 0 || end < start || end > source.length() || destinationPosition < 0 || destinationPosition > destination.length())
            throw new IllegalArgumentException("Invalid move range or destination");
        if (source == destination && destinationPosition > start && destinationPosition < end)
            throw new IllegalArgumentException("Destination is inside the moved range");
        String moved = source.getText().substring(start, end);
        var movedStyles = source.stylesInRange(start, end);
        if (source == destination) {
            if (destinationPosition == start || destinationPosition == end || start == end) return;
            source.deleteText(start, end);
            int adjusted = destinationPosition > end ? destinationPosition - (end - start) : destinationPosition;
            source.insertStyled(adjusted, moved, movedStyles);
        } else {
            source.deleteText(start, end);
            destination.insertStyled(destinationPosition, moved, movedStyles);
        }
    }
    /** Splits a paragraph and inserts the right half immediately after it. */
    public Paragraph split(Section section, int paragraphIndex, int position) {
        Objects.requireNonNull(section, "section");
        Paragraph original = section.getParagraph(paragraphIndex);
        Paragraph right = original.split(position);
        section.insertParagraph(paragraphIndex + 1, right);
        return right;
    }
    /** Merges the specified paragraph with its immediately following paragraph. */
    public void merge(Section section, int paragraphIndex) {
        Objects.requireNonNull(section, "section");
        if (paragraphIndex < 0 || paragraphIndex + 1 >= section.getParagraphs().size())
            throw new IllegalArgumentException("A following paragraph is required");
        section.getParagraph(paragraphIndex).append(section.getParagraph(paragraphIndex + 1));
        section.removeParagraph(paragraphIndex + 1);
    }
}
