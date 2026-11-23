// ECE 373 Final Project: Search
// Written by: Taysom Stolworthy (Tstolworthy)
// ECE 373 Object-Oriented Software Design, Section 001 - Fall 2026
// The University of Arizona
//------------------------------------------------------

package Search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

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
    }
    public CompositeCommand replaceAll(Document doc, String target, String replacement){

    }
}