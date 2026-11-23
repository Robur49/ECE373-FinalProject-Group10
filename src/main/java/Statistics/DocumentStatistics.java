package Statistics

import java.util.ArrayDeque;
import java.util.Deque;
import command.CompositeCommand;
import command.EditCommand;
import document.Document;
import document.Paragraph;
import document.Section;
import editor.DocumentEditor;



public class DocumentStatistics implements DocumentChangeListener {

    private int wordCount;
    private int characterCount;
    private int paragraphCount;

    @Override
    public void OnDocumentChanged(Document doc) {
        wordCount = 0;
        characterCount = 0;
        paragraphCount = 0;

        if (doc == null) {
            return;
        }

        Deque<Section> stack = new ArrayDeque<>(doc.getSections());
        while (!stack.isEmpty()) {
            Section current = stack.pop();

            String title = current.getStyledTitle().getText();
            if (title != null && !title.isEmpty()) {
                characterCount += title.length();
                String trimmedTitle = title.trim();
                if (!trimmedTitle.isEmpty()) {
                    wordCount += trimmedTitle.split("\\s+").length;
                }
            }

            for (Paragraph paragraph : current.getParagraphs()) {
                paragraphCount++;
                String text = paragraph.getText();
                if (text != null && !text.isEmpty()) {
                    characterCount += text.length();
                    String trimmedText = text.trim();
                    if (!trimmedText.isEmpty()) {
                        wordCount += trimmedText.split("\\s+").length;
                    }
                }
            }

            for (Section subSection : current.getSubsections()) {
                stack.push(subSection);
            }
        }
    }

    public String getStatistics(Document doc) {
        OnDocumentChanged(doc);
        return "Words: " + wordCount
                + " | Characters: " + characterCount
                + " | Paragraphs: " + paragraphCount;
    }
}