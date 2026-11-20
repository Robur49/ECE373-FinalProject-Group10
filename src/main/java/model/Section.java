//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package model;

import java.util.ArrayList;
import java.util.List;

// TEMPORARY CLASS: section titles and ordered paragraphs/subsections for testing.
public class Section {
    private final StyledText title;
    private final List<Object> children = new ArrayList<>(); // Each child is a Paragraph or Section.

    public Section(String text) {
        title = new StyledText(text); // Store the section title with formatting support.
    }

    public StyledText title() {
        return title; // Let the renderer read the formatted section title.
    }

    public List<Object> children() {
        return List.copyOf(children); // Keep paragraphs and subsections in the order they were added.
    }

    public Paragraph addParagraph(String text) {
        Paragraph paragraph = new Paragraph(text); // Make the paragraph.
        children.add(paragraph); // Add it after the existing children.
        return paragraph;
    }

    public Section addSection(String title) {
        Section section = new Section(title); // Make a new nested section.
        children.add(section); // Store it as a child of this section.
        return section;
    }

    public Section sectionAt(int oneBasedIndex) {
        int count = 0;
        for (Object child : children) {
            if (child instanceof Section) {
                count++; // Do not count paragraphs when finding a subsection.
                if (count == oneBasedIndex) {
                    return (Section) child;
                }
            }
        }
        throw new IllegalArgumentException("Subsection does not exist: " + oneBasedIndex);
    }
}
