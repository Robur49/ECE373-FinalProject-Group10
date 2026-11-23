// ECE 373 Final Project: Search
// Written by: Taysom Stolworthy (Tstolworthy)
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------

package Search;

import command.CompositeCommand;
import command.EditCommand;
import document.Document;
import document.Paragraph;
import document.Section;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Provides document searching and text replacement operations.
 */
public class SearchService {

public List<String> Find(Document doc, String query, boolean wholeWord) {
    Objects.requireNonNull(doc, "doc");
    Objects.requireNonNull(query, "query");

    List<String> results = new ArrayList<>();
    if (query.isEmpty()) {
        return results;
    }

    Deque<Section> sectionStack = new ArrayDeque<>();
    Deque<String> pathStack = new ArrayDeque<>();

    List<Section> topSections = doc.getSections();
    for (int i = topSections.size() - 1; i >= 0; i--) {
        sectionStack.push(topSections.get(i));
        pathStack.push(String.valueOf(i));
    }

    while (!sectionStack.isEmpty()) {
        Section currentSection = sectionStack.pop();
        String currentPath = pathStack.pop();

        // 1. Scan the section's title
        String titleText = currentSection.getStyledTitle().getText();
        int idx = titleText.indexOf(query, 0);
        while (idx != -1) {
            int end = idx + query.length();
            boolean validStart = !wholeWord || idx == 0 || !Character.isLetterOrDigit(titleText.charAt(idx - 1));
            boolean validEnd = !wholeWord || end == titleText.length() || !Character.isLetterOrDigit(titleText.charAt(end));
            if (validStart && validEnd) {
                results.add("Section [" + currentPath + "] Title at offset " + idx + ": \"" + query + "\"");
            }
            idx = titleText.indexOf(query, idx + 1);
        }

        // 2. Scan all body paragraphs in this section
        List<Paragraph> paragraphs = currentSection.getParagraphs();
        for (int p = 0; p < paragraphs.size(); p++) {
            String pText = paragraphs.get(p).getText();
            idx = pText.indexOf(query, 0);
            while (idx != -1) {
                int end = idx + query.length();
                boolean validStart = !wholeWord || idx == 0 || !Character.isLetterOrDigit(pText.charAt(idx - 1));
                boolean validEnd = !wholeWord || end == pText.length() || !Character.isLetterOrDigit(pText.charAt(end));
                if (validStart && validEnd) {
                    results.add("Section [" + currentPath + "] Paragraph " + p + " at offset " + idx + ": \"" + query + "\"");
                }
                idx = pText.indexOf(query, idx + 1);
            }
        }

        // 3. Push nested subsections in reverse order so they are visited top-to-bottom
        List<Section> subsections = currentSection.getSubsections();
        for (int s = subsections.size() - 1; s >= 0; s--) {
            sectionStack.push(subsections.get(s));
            pathStack.push(currentPath + "." + s);
        }
    }

    return results;
}



    public EditCommand replaceFirst(Document doc, String target, String replacement){
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(replacement, "replacement");
        if (target.isEmpty()) {
            throw new IllegalArgumentException("Target text cannot be empty");
        }

        Deque<Section> sectionStack = new ArrayDeque<>();
        Deque<String> pathStack = new ArrayDeque<>();

        List<Section> topSections = doc.getSections();
        for (int i = topSections.size() - 1; i >= 0; i--) {
            sectionStack.push(topSections.get(i));
            pathStack.push(String.valueOf(i));
        }

        while (!sectionStack.isEmpty()) {
            Section currentSection = sectionStack.pop();
            String currentPath = pathStack.pop();

            // 1. Check section title first
            String titleText = currentSection.getStyledTitle().getText();
            int idx = titleText.indexOf(target, 0);
            if (idx != -1) {
                return EditCommand.replace(
                        currentSection.getStyledTitle(),
                        idx,
                        idx + target.length(),
                        replacement);
            }

            // 2. Check body paragraphs in order
            List<Paragraph> paragraphs = currentSection.getParagraphs();
            for (int p = 0; p < paragraphs.size(); p++) {
                String pText = paragraphs.get(p).getText();
                idx = pText.indexOf(target, 0);
                if (idx != -1) {
                    return EditCommand.replace(
                            paragraphs.get(p),
                            idx,
                            idx + target.length(),
                            replacement);
                }
            }

            // 3. Push nested subsections
            List<Section> subsections = currentSection.getSubsections();
            for (int s = subsections.size() - 1; s >= 0; s--) {
                sectionStack.push(subsections.get(s));
                pathStack.push(currentPath + "." + s);
            }
        }

        return null;
    }
    public CompositeCommand replaceAll(Document doc, String target, String replacement){
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(replacement, "replacement");
        if (target.isEmpty()) {
            throw new IllegalArgumentException("Target text cannot be empty");
        }

        CompositeCommand composite = new CompositeCommand();
        Deque<Section> sectionStack = new ArrayDeque<>();
        Deque<String> pathStack = new ArrayDeque<>();

        List<Section> topSections = doc.getSections();
        for (int i = topSections.size() - 1; i >= 0; i--) {
            sectionStack.push(topSections.get(i));
            pathStack.push(String.valueOf(i));
        }

        while (!sectionStack.isEmpty()) {
            Section currentSection = sectionStack.pop();
            String currentPath = pathStack.pop();

            // 1. Collect all replacements in the section title
            String titleText = currentSection.getStyledTitle().getText();
            int offsetShift = 0;
            int idx = titleText.indexOf(target, 0);
            while (idx != -1) {
                composite.addCommand(EditCommand.replace(
                        currentSection.getStyledTitle(),
                        idx + offsetShift,
                        idx + offsetShift + target.length(),
                        replacement));
                offsetShift += replacement.length() - target.length();
                idx = titleText.indexOf(target, idx + target.length());
            }

            // 2. Collect all replacements in each body paragraph
            List<Paragraph> paragraphs = currentSection.getParagraphs();
            for (int p = 0; p < paragraphs.size(); p++) {
                String pText = paragraphs.get(p).getText();
                offsetShift = 0;
                idx = pText.indexOf(target, 0);
                while (idx != -1) {
                    composite.addCommand(EditCommand.replace(
                            paragraphs.get(p),
                            idx + offsetShift,
                            idx + offsetShift + target.length(),
                            replacement));
                    offsetShift += replacement.length() - target.length();
                    idx = pText.indexOf(target, idx + target.length());
                }
            }

            // 3. Push nested subsections
            List<Section> subsections = currentSection.getSubsections();
            for (int s = subsections.size() - 1; s >= 0; s--) {
                sectionStack.push(subsections.get(s));
                pathStack.push(currentPath + "." + s);
            }
        }

        return composite;
    }
}