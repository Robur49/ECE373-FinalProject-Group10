//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------

package document;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentElementTest {

    @Test
    void testDocumentElementTypes() {
        DocumentElement section = new Section("Section One");
        DocumentElement paragraph = new Paragraph("Hello World");

        assertTrue(section instanceof Section);
        assertTrue(paragraph instanceof Paragraph);
    }

    @Test
    void testGetChildren() {
        Section section = new Section("Section One");

        Paragraph paragraph = section.addParagraph("Hello World");
        Section subsection = section.addSubsection("Subsection One");

        List<DocumentElement> children = section.getChildren();

        assertEquals(2, children.size());
        assertSame(paragraph, children.get(0));
        assertSame(subsection, children.get(1));
    }

    @Test
    void testNestedChildren() {
        Section section = new Section("Section One");
        Section subsection = section.addSubsection("Subsection One");

        Paragraph paragraph = subsection.addParagraph("Nested text");

        assertSame(subsection, section.getChildren().get(0));
        assertSame(paragraph, subsection.getChildren().get(0));
    }

    @Test
    void testEmptySection() {
        Section section = new Section("Empty Section");

        assertTrue(section.getChildren().isEmpty());
    }

    @Test
    void testUnmodifiableChildren() {
        Section section = new Section("Section One");
        section.addParagraph("Hello World");

        assertThrows(UnsupportedOperationException.class, () -> {
            section.getChildren().clear();
        });
    }
}