//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import java.util.ArrayList;
import java.util.List;

// Tries to make the lines more even instead of filling every line completely.
public final class BalancedLineBreaker implements LineBreakingStrategy {

    @Override
    public String name() {
        return "balanced";
    }

    @Override
    public List<List<WordToken>> breakLines(List<WordToken> words, int width) {
        LineBreakingStrategy.validateWidth(width);

        int count = words.size(); // Total number of word pieces to arrange.
        double[] bestCost = new double[count + 1]; // Lowest remaining spacing cost from each word.
        int[] nextWord = new int[count]; // Remembers where each selected line should end.

        // Work backward to find the lowest cost for every possible line starting point.
        for (int i = count - 1; i >= 0; i--) {
            bestCost[i] = Double.POSITIVE_INFINITY; // Replace this when a valid line is found.
            int lineLength = 0; // Track the width of the possible line.

            for (int j = i; j < count; j++) {
                WordToken word = words.get(j);
                if (j > i && word.breakBefore()) {
                    break; // A long word was already split, so it needs a new line.
                }

                if (j > i) {
                    lineLength++; // Add the space separating two words.
                }
                lineLength += word.text().length(); // Add this word to the possible line length.

                if (lineLength > width) {
                    break;
                }

                int spaceLeft = width - lineLength; // Unused width left on this possible line.
                double cost = spaceLeft * spaceLeft + bestCost[j + 1]; // Penalize short lines and include the cost of later lines.

                if (cost < bestCost[i]) {
                    bestCost[i] = cost; // Remember the cheapest wrapping choice so far.
                    nextWord[i] = j + 1; // Save where the next line should begin.
                }
            }

            if (Double.isInfinite(bestCost[i])) {
                throw new IllegalArgumentException("Token longer than line width");
            }
        }

        List<List<WordToken>> lines = new ArrayList<>();
        int start = 0;
        while (start < count) {
            int end = nextWord[start]; // Retrieve the saved end point of this line.
            lines.add(new ArrayList<>(words.subList(start, end))); // Copy the selected words into a finished line.
            start = end;
        }
        return lines;
    }
}
