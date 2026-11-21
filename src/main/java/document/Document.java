package document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Root container for the structured document. */
public final class Document {
    private final List<Section> sections = new ArrayList<>();
    /** Creates an empty document. */ public Document() { }
    /** Adds a top-level section. */ public Section addSection(String title) {
        Section section = new Section(title);
        sections.add(section);
        return section;
    }
    /** Returns the top-level sections without exposing a mutable list. */ public List<Section> getSections() { return Collections.unmodifiableList(sections); }
    /** Returns a top-level section at a zero-based index. */ public Section getSection(int index) { return sections.get(index); }
    /** Returns the total number of paragraphs, including nested sections. */ public int countParagraphs() {
        int total = 0;
        for (Section section : sections) total += section.countParagraphs();
        return total;
    }
    /** Returns a section identified by a path of zero-based subsection indices. */
    public Section findSection(int... path) {
        Objects.requireNonNull(path, "path");
        if (path.length == 0) throw new IllegalArgumentException("Section path cannot be empty");
        Section section = getSection(path[0]);
        for (int i = 1; i < path.length; i++) section = section.getSubsection(path[i]);
        return section;
    }
    /** Returns an independent deep copy. */ public Document copy() {
        Document result = new Document();
        for (Section section : sections) result.sections.add(section.copy());
        return result;
    }
}
