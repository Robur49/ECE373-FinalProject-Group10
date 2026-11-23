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


}