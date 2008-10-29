package org.limewire.ui.swing.components;

import java.awt.Toolkit;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class NumericTextField extends JTextField {

    public NumericTextField(int columns) {
        super(columns);
        addFilter();
    }

    private void addFilter() {
        ((AbstractDocument) this.getDocument()).setDocumentFilter(new NumericDocumentFilter());
    }

    class NumericDocumentFilter extends DocumentFilter {
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {

            if (string == null)
                return;
            if (isStringNumeric(string)) {
                super.insertString(fb, offset, string, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null)
                return;
            if (isStringNumeric(text)) {
                super.replace(fb, offset, length, text, attrs);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        private boolean isStringNumeric(String string) {
            try {
                Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return false;
            }
            return true;
        }
    }
}
