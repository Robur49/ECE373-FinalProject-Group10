//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import java.util.ArrayList;
import java.util.List;

// Adds as many words as possible to each line before starting the next line.
public final class GreedyLineBreaker implements LineBreakingStrategy {

    @Override
    public String name() {
        return "greedy";
    }

    @Override
    public List<List<WordToken>> breakLines(List<WordToken> words, int width) {
        LineBreakingStrategy.validateWidth(width);

        List<List<WordToken>> lines = new ArrayList<>(); // Save each finished output line.
        List<WordToken> currentLine = new ArrayList<>(); // Collect words for the current line.
        int usedWidth = 0; // Number of characters already used on this line.

        for (WordToken word : words) {
            int wordLength = word.text().length(); // Check how much space this word needs.
            if (wordLength > width) {
                throw new IllegalArgumentException("Token longer than line width");
            }

            boolean startNewLine = !currentLine.isEmpty() &&
                    (word.breakBefore() || usedWidth + 1 + wordLength > width);

            if (startNewLine) {
                lines.add(currentLine); // The old line is full; save it.
                currentLine = new ArrayList<>();
                usedWidth = 0;
            }

            if (!currentLine.isEmpty()) {
                usedWidth++; // Add one space between words.
            }
            currentLine.add(word); // Add this word to the current line.
            usedWidth += wordLength; // Count the characters just added.
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }
        return lines;
    }
}
