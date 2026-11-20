//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import java.util.List;

// Each line breaker uses this interface but can have its own algorithm.
public interface LineBreakingStrategy {

    List<List<WordToken>> breakLines(List<WordToken> words, int width); // Group words into lines using the selected method.

    String name(); // Returns the algorithm name shown to the user.

    static void validateWidth(int width) {
        if (width < 1) { // A line must be able to hold at least one character.
            throw new IllegalArgumentException("Width must be at least 1");
        }
    }
}
