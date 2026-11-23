package Search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contains unit tests for SearchService.
 */
class SearchServiceTest {

    @Test
    void FindWholeWord() {
        Document doc = new Document();

        Section section = doc.addSection("Routine");
        section.addParagraph("I woke up this morning");

       SearchService service = new SearchService();

       List<String> test = service.Find( doc, "woke", true);
        assertEquals(List.of("Section [0] Paragraph 0 at offset 2: \"woke\""), test);
    }

    @Test
    void Findsentence() {
        Document doc = new Document();

        Section section = doc.addSection("Routine");
        section.addParagraph("I woke up this morning. It was hard getting out of bed");

        SearchService service = new SearchService();

        List<String> test = service.Find( doc, "I woke up this morning.", true);
        assertEquals(List.of("Section [0] Paragraph 0 at offset 0: \"I woke up this morning.\""), test);
    }

    @Test
    void findSubstring() {
        Document doc = new Document();

        Section section = doc.addSection("animals");
        section.addParagraph("woman");

        SearchService service = new SearchService();

        List<String> test = service.Find(doc, "man", false);
        assertEquals(List.of("Section [0] Paragraph 0 at offset 2: \"man\""), test);
    }

    @Test
    void wholeWordRejectsPartialMatch() {
        Document doc = new Document();

        Section section = doc.addSection("animals");
        section.addParagraph("woman");

        SearchService service = new SearchService();

        List<String> test = service.Find(doc, "man", true);
        assertEquals(List.of(), test);
    }

    @Test
    void caseSensitive() {
        Document doc = new Document();

        Section section = doc.addSection("Schedule");
        section.addParagraph("Routine");

        SearchService service = new SearchService();

        List<String> test = service.Find(doc, "routine", false);
        assertEquals(List.of(), test);
    }

    @Test
    void FindRepeatingWholeWord() {
        Document doc = new Document();

        Section section = doc.addSection("Routine");
        section.addParagraph("I woke up this morning. woke");

        SearchService service = new SearchService();

        List<String> test = service.Find(doc, "woke", true);
        assertEquals(
                List.of(
                        "Section [0] Paragraph 0 at offset 2: \"woke\"",
                        "Section [0] Paragraph 0 at offset 24: \"woke\""),
                test);
    }

    @Test
    void findMatchInSectionTitle() {
        Document doc = new Document();
        doc.addSection("Morning Routine");

        SearchService service = new SearchService();
        List<String> test = service.Find(doc, "Morning", true);

        assertEquals(List.of("Section [0] Title at offset 0: \"Morning\""), test);
    }

    @Test
    void findMatchesInNestedSubsections() {
        Document doc = new Document();
        Section topSection = doc.addSection("Chapter 1");

        Section subSection = topSection.addSubsection("Section 1.1");
        subSection.addParagraph("First target");

        Section subSubSection = subSection.addSubsection("Section 1.1.1");
        subSubSection.addParagraph("Second target");

        SearchService service = new SearchService();
        List<String> test = service.Find(doc, "target", true);

        assertEquals(
                List.of(
                        "Section [0.0] Paragraph 0 at offset 6: \"target\"",
                        "Section [0.0.0] Paragraph 0 at offset 7: \"target\""),
                test);
    }

    @Test
    void findWordSplitAcrossFormattingRuns() {
        Document doc = new Document();
        Section section = doc.addSection("Lunch");
        Paragraph paragraph = section.addParagraph("I ate a hotdog");

        // Bold "hot" (indices 8..11) while leaving "dog" plain so the word spans two TextRuns
        paragraph.formatRange(8, 11, new TextStyle(true, false, false));

        SearchService service = new SearchService();
        List<String> test = service.Find(doc, "hotdog", true);

        assertEquals(List.of("Section [0] Paragraph 0 at offset 8: \"hotdog\""), test);
    }

    @Test
    void replaceFirstChangesOnlyFirstMatchAndUndoes() {
        Document doc = new Document();
        Section section = doc.addSection("My dog");
        Paragraph paragraph = section.addParagraph("The dog barked");

        SearchService service = new SearchService();
        EditCommand cmd = service.replaceFirst(doc, "dog", "cat");

        cmd.execute();
        assertEquals("My cat", section.getStyledTitle().getText());
        assertEquals("The dog barked", paragraph.getText());

        cmd.undo();
        assertEquals("My dog", section.getStyledTitle().getText());
        assertEquals("The dog barked", paragraph.getText());
    }

    @Test
    void replaceFirstReturnsNullWhenNoMatch() {
        Document doc = new Document();
        Section section = doc.addSection("Animals");
        section.addParagraph("The dog barked");

        SearchService service = new SearchService();
        EditCommand cmd = service.replaceFirst(doc, "elephant", "lion");

        assertNull(cmd);
    }

    @Test
    void replaceAllHandlesChangingWordLengths() {
        Document doc = new Document();
        Section section = doc.addSection("Pets");
        Paragraph paragraph = section.addParagraph("one cat and one cat");

        SearchService service = new SearchService();
        CompositeCommand cmd = service.replaceAll(doc, "cat", "elephant");

        cmd.execute();
        assertEquals("one elephant and one elephant", paragraph.getText());

        cmd.undo();
        assertEquals("one cat and one cat", paragraph.getText());
    }

    @Test
    void replaceAllAcrossTitlesParagraphsAndSubsectionsAndUndoes() {
        Document doc = new Document();
        Section topSection = doc.addSection("Old Title");
        Paragraph topParagraph = topSection.addParagraph("Old body text");
        Section subSection = topSection.addSubsection("Old Subheading");
        Paragraph subParagraph = subSection.addParagraph("Old nested text");

        SearchService service = new SearchService();
        CompositeCommand cmd = service.replaceAll(doc, "Old", "New");

        cmd.execute();
        assertEquals("New Title", topSection.getStyledTitle().getText());
        assertEquals("New body text", topParagraph.getText());
        assertEquals("New Subheading", subSection.getStyledTitle().getText());
        assertEquals("New nested text", subParagraph.getText());

        cmd.undo();
        assertEquals("Old Title", topSection.getStyledTitle().getText());
        assertEquals("Old body text", topParagraph.getText());
        assertEquals("Old Subheading", subSection.getStyledTitle().getText());
        assertEquals("Old nested text", subParagraph.getText());
    }

}