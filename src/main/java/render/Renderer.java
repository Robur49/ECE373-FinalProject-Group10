//------------------------------------------------------
// ECE 373 Final Project: Document Editor
// Written by: Noah Perry 23964993
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------
package render;

import document.Document;

// This lets us switch between different ways of displaying the document.
public interface Renderer {

    String render(Document document, int width, LineBreakingStrategy strategy); // Display the document without editing it.

    String name(); // Identify which view the user selected.
}
