package document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/** Section with an editable title, paragraphs, and arbitrarily nested subsections. */
public final class Section implements DocumentElement {
    // Returns all paragraphs and subsections as document elements.
    public List<DocumentElement> getChildren() {
        List<DocumentElement> children = new ArrayList<>();
        children.addAll(paragraphs);
        children.addAll(subsections);
        return Collections.unmodifiableList(children);
    }
    private final Paragraph title;
    private final List<Paragraph> paragraphs = new ArrayList<>();
    private final List<Section> subsections = new ArrayList<>();
    /** Creates a section with a title. */ public Section(String title) { this.title = new Paragraph(Objects.requireNonNull(title, "title")); }
    /** Returns the plain title. */ public String getTitle() { return title.getText(); }
    /** Returns the styled title. */ public Paragraph getStyledTitle() { return title; }
    /** Formats a range of title characters. */ public void formatTitle(int start, int end, UnaryOperator<TextStyle> operation) { title.formatRange(start, end, operation); }
    /** Adds a paragraph. */ public Paragraph addParagraph(String text) {
        Paragraph paragraph = new Paragraph(text);
        paragraphs.add(paragraph);
        return paragraph;
    }
    /** Adds a nested subsection. */ public Section addSubsection(String title) {
        Section section = new Section(title);
        subsections.add(section);
        return section;
    }
    /** Inserts a subsection at a specified position for undo and redo. */
    public void insertSubsection(int index, Section section) {
        subsections.add(index, Objects.requireNonNull(section, "section"));
    }
    /** Removes a subsection at a specified position. */
    public void removeSubsection(int index) {
        subsections.remove(index);
    }
    /** Returns paragraphs without allowing list mutation. */ public List<Paragraph> getParagraphs() { return Collections.unmodifiableList(paragraphs); }
    /** Returns nested sections without allowing list mutation. */ public List<Section> getSubsections() { return Collections.unmodifiableList(subsections); }
    /** Returns a paragraph at its zero-based index. */ public Paragraph getParagraph(int index) { return paragraphs.get(index); }
    /** Returns a nested section at its zero-based index. */ public Section getSubsection(int index) { return subsections.get(index); }
    /** Returns the paragraph count including all nested sections. */ public int countParagraphs() {
        int count = paragraphs.size();
        for (Section child : subsections) count += child.countParagraphs();
        return count;
    }
    public void insertParagraph(int index, Paragraph paragraph) { paragraphs.add(index, Objects.requireNonNull(paragraph)); }
    public void removeParagraph(int index) { paragraphs.remove(index); }
    /** Returns an independent deep copy of this section. */ public Section copy() {
        Section result = new Section(getTitle());
        result.title.deleteText(0, result.title.length());
        result.title.append(title);
        for (Paragraph paragraph : paragraphs) result.paragraphs.add(paragraph.copy());
        for (Section child : subsections) result.subsections.add(child.copy());
        return result;
    }
}
