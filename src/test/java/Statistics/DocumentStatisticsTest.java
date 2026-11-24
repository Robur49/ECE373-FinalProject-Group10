package Statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import document.Document;
import document.Section;
import org.junit.jupiter.api.Test;

class DocumentStatisticsTest {

    @Test
    void emptyDocumentReturnsZeroCounts() {
        Document doc = new Document();
        DocumentStatistics stats = new DocumentStatistics();

        assertEquals("Words: 0 | Characters: 0 | Paragraphs: 0", stats.getStatistics(doc));
    }

    @Test
    void countsSingleSectionTitleAndParagraph() {
        Document doc = new Document();
        Section section = doc.addSection("Routine"); // 1 word, 7 chars
        section.addParagraph("I woke up"); // 3 words, 9 chars, 1 paragraph

        DocumentStatistics stats = new DocumentStatistics();

        assertEquals("Words: 4 | Characters: 16 | Paragraphs: 1", stats.getStatistics(doc));
    }

    @Test
    void countsMultipleParagraphsAndNestedSubsections() {
        Document doc = new Document();
        Section topSection = doc.addSection("Chapter 1"); // 2 words, 9 chars
        topSection.addParagraph("Hello world"); // 2 words, 11 chars, 1 paragraph

        Section subSection = topSection.addSubsection("Part A"); // 2 words, 6 chars
        subSection.addParagraph("More text here"); // 3 words, 14 chars, 1 paragraph

        DocumentStatistics stats = new DocumentStatistics();

        assertEquals("Words: 9 | Characters: 40 | Paragraphs: 2", stats.getStatistics(doc));
    }

    @Test
    void ignoresWhitespaceOnlyParagraphInWordCount() {
        Document doc = new Document();
        Section section = doc.addSection("Note"); // 1 word, 4 chars
        section.addParagraph("   "); // 0 words, 3 chars, 1 paragraph

        DocumentStatistics stats = new DocumentStatistics();

        assertEquals("Words: 1 | Characters: 7 | Paragraphs: 1", stats.getStatistics(doc));
    }

    @Test
    void updatesStatisticsWhenDocumentChanges() {
        Document doc = new Document();
        DocumentStatistics stats = new DocumentStatistics();

        assertEquals("Words: 0 | Characters: 0 | Paragraphs: 0", stats.getStatistics(doc));

        Section section = doc.addSection("Title"); // 1 word, 5 chars
        section.addParagraph("One two"); // 2 words, 7 chars, 1 paragraph
        stats.OnDocumentChanged(doc);

        assertEquals("Words: 3 | Characters: 12 | Paragraphs: 1", stats.getStatistics(doc));
    }

    @Test
    void handlesNullDocumentSafely() {
        DocumentStatistics stats = new DocumentStatistics();

        assertEquals("Words: 0 | Characters: 0 | Paragraphs: 0", stats.getStatistics(null));
    }
}