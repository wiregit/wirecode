package org.limewire.ui.swing.components;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * This class is a DocumentFilter that limits the length of a Document
 * which often serves as a model for a TextComponent.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class DocumentLengthFilter extends DocumentFilter {

    private int maxLength;

    /**
     * Creates a DocumentLengthFilter.
     * @param limit the maximum number of characters allowed
     */
    public DocumentLengthFilter(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * This method is called when characters are inserted into the document.
     */
    @Override
    public void insertString(
        DocumentFilter.FilterBypass fb,
        int offset,
        String str,
        AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, str, attr);
    }

    /**
     * This method is called when characters in the document
     * are replaced with other characters.
     */
    @Override
    public void replace(
        DocumentFilter.FilterBypass fb,
        int offset,
        int length,
        String str,
        AttributeSet attrs) throws BadLocationException {

        int newLength = fb.getDocument().getLength() - length + str.length();
        if (newLength > maxLength) {
            throw new BadLocationException(
                "New characters exceeds max size of document", offset);
        }

        fb.replace(offset, length, str, attrs);
    }
}