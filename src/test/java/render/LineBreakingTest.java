//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LineBreakingTest {

    @Test
    void testGreedyLineBreaking() {
        List<WordToken> words = List.of(
                new WordToken("Hello", 0, false),
                new WordToken("my", 6, false),
                new WordToken("friend", 9, false));

        List<List<WordToken>> lines = new GreedyLineBreaker().breakLines(words, 8); // Use eight characters per line.

        assertEquals(2, lines.size()); // One line for "Hello my" and one for "friend".
        assertEquals(2, lines.get(0).size()); // "Hello my" fits exactly in width 8.
        assertEquals("friend", lines.get(1).get(0).text());
    }

    @Test
    void testBalancedUsesAnotherAlgorithm() {
        List<WordToken> words = List.of(
                new WordToken("aaa", 0, false),
                new WordToken("bbb", 4, false),
                new WordToken("ccc", 8, false),
                new WordToken("ddd", 12, false),
                new WordToken("eee", 16, false));

        List<List<WordToken>> greedy = new GreedyLineBreaker().breakLines(words, 11); // Fill each line in order.
        List<List<WordToken>> balanced = new BalancedLineBreaker().breakLines(words, 11); // Compare a more even layout.

        assertEquals(3, greedy.get(0).size()); // Greedy fills the first line.
        assertEquals(2, balanced.get(0).size()); // Balanced leaves a more even final line.
    }

    @Test
    void testBadWidth() {
        assertThrows(IllegalArgumentException.class, // A line width of zero is invalid.
                () -> new GreedyLineBreaker().breakLines(List.of(), 0));
    }
}
