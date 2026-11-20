//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package model;

import java.util.ArrayList;
import java.util.List;

// TEMPORARY CLASS: lets the renderer run until the team's Document.java is ready.
public class Document {
    private final List<Section> sections = new ArrayList<>(); // Store all top-level sections.

    public List<Section> sections() {
        return List.copyOf(sections); // Return sections without letting other classes modify this list.
    }

    public String addSection(String parentPath, String title) {
        if (parentPath.equalsIgnoreCase("root") || parentPath.equals("-")) {
            sections.add(new Section(title)); // Add a new top-level section.
            return String.valueOf(sections.size()); // Give back its section number.
        }

        Section parent = section(parentPath); // Find the parent section first.
        parent.addSection(title); // Create a subsection inside that section.
        int sectionCount = 0;
        for (Object child : parent.children()) {
            if (child instanceof Section) {
                sectionCount++; // Count only subsections, not paragraphs.
            }
        }
        return parentPath + "." + sectionCount;
    }

    public Section section(String path) {
        if (path == null || !path.matches("[1-9][0-9]*(\\.[1-9][0-9]*)*")) {
            throw new IllegalArgumentException("Section path should look like 1 or 1.2");
        }
        String[] parts = path.split("\\."); // Split 1.2 into section 1 and subsection 2.
        int firstIndex = Integer.parseInt(parts[0]) - 1;
        if (firstIndex >= sections.size()) {
            throw new IllegalArgumentException("Section does not exist: " + path);
        }
        Section current = sections.get(firstIndex);
        for (int i = 1; i < parts.length; i++) {
            current = current.sectionAt(Integer.parseInt(parts[i])); // Walk into the nested sections.
        }
        return current;
    }
}
