package parser;

import document.Paragraph;
import document.TextStyle;
import editor.DocumentEditor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests commands through the same parser used by scripts and the console. */
class CommandParserTest {

    /** A new document can be created entirely through the command interface. */
    @Test
    void createsNestedDocument() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section \"Introduction\"");
        parser.execute("subsection 0 \"Background\"");
        parser.execute("paragraph 0.0 \"Hello world\"");

        assertEquals("Background", editor.getDocument().findSection(0, 0).getTitle());
        assertEquals("Hello world", editor.getDocument().findSection(0, 0)
                .getParagraph(0).getText());
    }

    /** Structure edits are recorded in the ordinary undo/redo history. */
    @Test
    void undoAndRedoStructure() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Title");
        parser.execute("paragraph 0 Hello");
        parser.execute("undo");
        assertTrue(editor.getDocument().findSection(0).getParagraphs().isEmpty());
        parser.execute("undo");
        assertTrue(editor.getDocument().getSections().isEmpty());
        parser.execute("redo");
        parser.execute("redo");
        assertEquals("Hello", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
    }

    /** All position-based editing operations use section paths and zero-based indices. */
    @Test
    void insertDeleteReplaceAndMove() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Title");
        parser.execute("paragraph 0 \"abc def\"");
        parser.execute("paragraph 0 XYZ");
        parser.execute("insert 0 0 3 \"!\"");
        assertEquals("abc! def", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
        parser.execute("delete 0 0 3 4");
        parser.execute("replace 0 0 4 7 ghi");
        assertEquals("abc ghi", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
        parser.execute("move 0 0 4 7 0 1 3");
        assertEquals("abc ", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
        assertEquals("XYZghi", editor.getDocument().findSection(0)
                .getParagraph(1).getText());
    }

    /** Split and merge follow the existing undoable EditCommand operations. */
    @Test
    void splitsMergesAndUndoes() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Title");
        parser.execute("paragraph 0 HelloWorld");
        parser.execute("split 0 0 5");
        assertEquals("World", editor.getDocument().findSection(0)
                .getParagraph(1).getText());
        parser.execute("merge 0 0");
        assertEquals("HelloWorld", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
        parser.execute("undo");
        assertEquals(2, editor.getDocument().findSection(0).getParagraphs().size());
    }

    /** Formatting both titles and body text preserves characters. */
    @Test
    void formatsTextAndSectionTitle() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Hello");
        parser.execute("paragraph 0 \"bold text\"");
        parser.execute("format 0 0 0 4 bold on");
        parser.execute("format-title 0 0 5 color blue");
        Paragraph paragraph = editor.getDocument().findSection(0).getParagraph(0);
        TextStyle style = paragraph.getRuns().get(0).getStyle();
        assertTrue(style.isBold());
        assertEquals("Hello", editor.getDocument().findSection(0).getTitle());
        assertEquals("blue", editor.getDocument().findSection(0)
                .getStyledTitle().getRuns().get(0).getStyle().getColor());
    }

    /** Search/replace, renderer settings, and reporting work through the parser. */
    @Test
    void searchReplaceRenderAndStats() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Report");
        parser.execute("paragraph 0 \"program program\"");
        assertTrue(parser.execute("find program whole").contains("Paragraph 0"));
        assertEquals("All matches replaced.",
                parser.execute("replace-all program system"));
        assertEquals("system system", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
        parser.execute("renderer plain");
        parser.execute("wrap balanced");
        parser.execute("width 40");
        assertTrue(parser.execute("show").contains("system system"));
        assertEquals("Words: 3, Characters: 19, Paragraphs: 1",
                parser.execute("stats"));
        parser.execute("undo");
        assertEquals("program program", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
    }

    /** A checkpoint restores the document and can be undone. */
    @Test
    void checkpointRestore() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Title");
        parser.execute("paragraph 0 Old");
        parser.execute("checkpoint saved");
        parser.execute("replace 0 0 0 3 New");
        parser.execute("restore saved");
        assertEquals("Old", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
        parser.execute("undo");
        assertEquals("New", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
    }

    /** Factories create new operations for a macro replay. */
    @Test
    void recordsAndReplaysMacro() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Title");
        parser.execute("paragraph 0 A");
        parser.execute("macro-start append");
        parser.execute("insert 0 0 1 B");
        parser.execute("macro-stop");
        parser.execute("macro-play append");
        assertEquals("ABB", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
        parser.execute("undo");
        assertEquals("AB", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
    }

    /** Invalid commands and ranges must not corrupt existing document text. */
    @Test
    void invalidInputDoesNotChangeDocument() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        parser.execute("section Title");
        parser.execute("paragraph 0 \"Stay intact\"");
        assertThrows(IllegalArgumentException.class,
                () -> parser.execute("insert 0 0 bad text"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.execute("delete 0 0 2 99"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.execute("format 0 0 0 4 bold maybe"));
        assertThrows(IllegalArgumentException.class,
                () -> parser.execute("bogus"));
        assertEquals("Stay intact", editor.getDocument().findSection(0)
                .getParagraph(0).getText());
    }

    /** Quotes, whitespace, comments, and named paths are parsed consistently. */
    @Test
    void handlesQuotedArgumentsAndComments() {
        DocumentEditor editor = new DocumentEditor();
        CommandParser parser = new CommandParser(editor);
        assertEquals("", parser.execute("# ignored"));
        assertEquals("", parser.execute(" "));
        parser.execute("section \"One \\\"quoted\\\" title\"");
        assertEquals("One \"quoted\" title",
                editor.getDocument().getSection(0).getTitle());
        assertThrows(IllegalArgumentException.class,
                () -> parser.execute("section \"unfinished"));
        assertFalse(CommandParser.help().isBlank());
    }
}
