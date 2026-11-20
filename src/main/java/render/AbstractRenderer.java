//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import model.Document; // just guessing what these will be named
import model.Paragraph;
import model.Section;
import model.StyledText;
import model.TextRun;
import model.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Handles the document layout so plain and markup rendering can share the same code.
abstract class AbstractRenderer implements Renderer {

    private static final Pattern WORD = Pattern.compile("\\S+"); // Match groups of non-space characters.

    @Override
    public final String render(Document document, int width, LineBreakingStrategy strategy) {
        LineBreakingStrategy.validateWidth(width); // Reject widths that cannot fit any characters.
        StringBuilder output = new StringBuilder(); // Build the final output a line at a time.

        for (Section section : document.sections()) {
            renderSection(section, 1, width, strategy, output); // Begin at the top section level.
        }
        return output.toString().stripTrailing();
    }

    private void renderSection(Section section, int depth, int width,
                               LineBreakingStrategy strategy, StringBuilder output) {
        String indent = "  ".repeat(Math.max(0, depth - 1)); // Add indentation for nested sections.
        String titlePrefix = titlePrefix(depth);
        String textPrefix = indent + "  "; // Indent paragraphs below their section title.

        renderText(section.title(), titlePrefix, textPrefix, width, strategy, output); // Print the title before its children.

        for (Object child : section.children()) {
            if (child instanceof Paragraph) {
                Paragraph paragraph = (Paragraph) child;
                renderText(paragraph.text(), textPrefix, textPrefix, width, strategy, output); // Print the paragraph using the selected view.
            }
            else if (child instanceof Section) {
                Section subsection = (Section) child;
                renderSection(subsection, depth + 1, width, strategy, output); // Handle nested sections.
            }
        }
    }

    private void renderText(StyledText text, String firstPrefix, String otherPrefix,
                            int width, LineBreakingStrategy strategy, StringBuilder output) {
        List<WordToken> words = getWords(text.plainText(), width); // Convert the paragraph into words with source positions.

        if (words.isEmpty()) {
            output.append(firstPrefix).append('\n'); // Keep an empty paragraph as a blank output line.
            return;
        }

        List<List<WordToken>> lines = strategy.breakLines(words, width); // Let the chosen algorithm decide where lines end.
        boolean firstLine = true;

        for (List<WordToken> line : lines) {
            if (firstLine) {
                output.append(firstPrefix); // Only the first line gets the title/initial prefix.
                firstLine = false;
            }
            else {
                output.append(otherPrefix); // Continuation lines use the normal indentation.
            }

            boolean firstWord = true;
            for (WordToken word : line) {
                if (!firstWord) {
                    output.append(' '); // Separate words that share the same output line.
                }
                firstWord = false;

                // Get the formatted characters for this word, even if it has multiple styles.
                int start = word.sourceStart();
                int end = start + word.text().length();
                StyledText selected = text.slice(start, end); // Recover the original styles for this one word.

                for (TextRun run : selected.runs()) {
                    output.append(encode(run.text(), run.style())); // Plain and markup views handle the run differently.
                }
            }
            output.append('\n');
        }
    }

    private List<WordToken> getWords(String text, int width) {
        List<WordToken> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text); // Find each word and its offset in the source text.

        while (matcher.find()) {
            String word = matcher.group();

            // Split long words so that we do not go past the line width.
            for (int offset = 0; offset < word.length(); offset += width) {
                int end = Math.min(offset + width, word.length()); // Do not make a word piece wider than the limit.
                String part = word.substring(offset, end);
                boolean newLine = offset > 0; // Continued pieces of a long word must start a new line.
                words.add(new WordToken(part, matcher.start() + offset, newLine)); // Remember where the piece came from for formatting.
            }
        }
        return words;
    }

    // Each renderer decides how section titles are marked in its view.
    protected abstract String titlePrefix(int depth);

    protected abstract String encode(String text, TextStyle style);
}
