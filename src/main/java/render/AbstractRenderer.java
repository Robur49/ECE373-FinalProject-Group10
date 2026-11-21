//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import document.Document;
import document.Paragraph;
import document.Section;
import document.TextRun;
import document.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Shared rendering code used by both the plain-text and markup views.
abstract class AbstractRenderer implements Renderer {

    private static final Pattern WORD = Pattern.compile("\\S+"); // Find each word in a paragraph.

    @Override
    public final String render(Document document, int width, LineBreakingStrategy strategy) {
        Objects.requireNonNull(document, "document"); // Make sure a document was provided.
        Objects.requireNonNull(strategy, "strategy"); // Make sure a wrapping method was selected.
        LineBreakingStrategy.validateWidth(width); // Reject invalid line widths.

        StringBuilder output = new StringBuilder(); // Store the displayed document.
        for (Section section : document.getSections()) {
            renderSection(section, 1, width, strategy, output); // Start at the outermost section.
        }
        return output.toString().stripTrailing(); // Return the finished display as a String.
    }

    private void renderSection(Section section, int depth, int width,
                               LineBreakingStrategy strategy, StringBuilder output) {
        String indent = "  ".repeat(Math.max(0, depth - 1)); // Indent nested sections.
        String textPrefix = indent + "  "; // Indent paragraphs below the title.

        renderText(section.getStyledTitle(), titlePrefix(depth), textPrefix,
                width, strategy, output); // Display the styled section title.

        // Matthew's Section stores paragraphs and subsections in separate lists.
        for (Paragraph paragraph : section.getParagraphs()) {
            renderText(paragraph, textPrefix, textPrefix,
                    width, strategy, output); // Display each paragraph in this section.
        }

        for (Section child : section.getSubsections()) {
            renderSection(child, depth + 1, width, strategy, output); // Display nested sections recursively.
        }
    }

    private void renderText(Paragraph paragraph, String firstPrefix, String otherPrefix,
                            int width, LineBreakingStrategy strategy, StringBuilder output) {
        String text = paragraph.getText(); // Read the paragraph without changing it.
        List<WordToken> words = getWords(text, width); // Remember each word's original position.

        if (words.isEmpty()) {
            output.append(firstPrefix).append('\n'); // Display an empty paragraph as a blank line.
            return;
        }

        List<TextRun> runs = paragraph.getRuns(); // Each run stores one section of shared formatting.
        int[] runStarts = new int[runs.size()]; // Remember where each run starts in the paragraph.
        int characterPosition = 0;
        for (int i = 0; i < runs.size(); i++) {
            runStarts[i] = characterPosition; // Store the character offset of this run.
            characterPosition += runs.get(i).length(); // Find the offset of the following run.
        }

        List<List<WordToken>> lines = strategy.breakLines(words, width); // Wrap words using the chosen method.
        boolean firstLine = true;

        for (List<WordToken> line : lines) {
            if (firstLine) {
                output.append(firstPrefix); // First line uses the normal paragraph or title prefix.
                firstLine = false;
            }
            else {
                output.append(otherPrefix); // Additional lines use the continuation indentation.
            }

            boolean firstWord = true;
            for (WordToken word : line) {
                if (!firstWord) {
                    output.append(' '); // Add a space between words on the same line.
                }
                firstWord = false;

                // Display the word with its original formatting, even if it spans multiple runs.
                appendStyledWord(word, runs, runStarts, output);
            }
            output.append('\n'); // Move to the next wrapped line.
        }
    }

    private void appendStyledWord(WordToken word, List<TextRun> runs,
                                  int[] runStarts, StringBuilder output) {
        int start = word.sourceStart(); // Position where the word started in the source text.
        int end = start + word.text().length(); // End of that word, not including the next character.
        int runIndex = findRun(runStarts, start); // Find the style run containing the first character.

        while (start < end) {
            TextRun run = runs.get(runIndex); // Get the text and formatting for this run.
            int runEnd = runStarts[runIndex] + run.length(); // Last position covered by the run.
            int pieceEnd = Math.min(end, runEnd); // Only display the part inside this word.
            String piece = run.getText().substring(start - runStarts[runIndex],
                    pieceEnd - runStarts[runIndex]); // Extract the original characters.
            output.append(encode(piece, run.getStyle())); // Let the renderer show or hide the style.
            start = pieceEnd; // Continue at the next part of the word.
            runIndex++; // If more text remains, it belongs to the next run.
        }
    }

    private int findRun(int[] starts, int position) {
        int low = 0;
        int high = starts.length - 1;
        while (low <= high) {
            int middle = (low + high) / 2; // Look in the middle instead of searching every run.
            if (starts[middle] <= position) {
                low = middle + 1; // The matching run is here or later.
            }
            else {
                high = middle - 1; // Look earlier in the list.
            }
        }
        return high; // Index of the last run starting at or before this position.
    }

    private List<WordToken> getWords(String text, int width) {
        List<WordToken> words = new ArrayList<>(); // Save the words with their source positions.
        Matcher matcher = WORD.matcher(text); // Search for groups of non-whitespace characters.

        while (matcher.find()) {
            String word = matcher.group(); // Get the next word.
            for (int offset = 0; offset < word.length(); offset += width) {
                int end = Math.min(offset + width, word.length()); // Split words too long for one line.
                String part = word.substring(offset, end);
                boolean breakBefore = offset > 0; // Continued word pieces must start on a new line.
                words.add(new WordToken(part, matcher.start() + offset, breakBefore));
            }
        }
        return words;
    }

    protected abstract String titlePrefix(int depth); // Let each view choose its section-title labels.

    protected abstract String encode(String text, TextStyle style); // Let each view handle formatting.
}
