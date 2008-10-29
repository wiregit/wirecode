package org.limewire.ui.swing.components;

import java.awt.Toolkit;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * A JTextField component that only accepts integer values as input. Any other
 * input will not be added and a system beep will be issued. Additionally, 
 * a max number of columns can be set. This will not allow values of more
 * than X places to be entered.
 */
public class NumericTextField extends JTextField {

    private int maxColumns;
    
    /**
     * Create a Textfield with a specified number of columns displayed.
     */
    public NumericTextField(int columns) {
        this(columns, Integer.MAX_VALUE);
    }

    /**
     * Create a Textfield with a specified number of columns displayed,
     * and a maximum number of columns to be entered.
     */
    public NumericTextField(int columns, int maxColumns) {
        super(columns);
        addFilter();
        
        this.maxColumns = maxColumns;
    }

    private void addFilter() {
        ((AbstractDocument) this.getDocument()).setDocumentFilter(new NumericDocumentFilter());
    }

    class NumericDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {

            if (string == null)
                return;
            if (isStringNumeric(string) && offset + string.length() < maxColumns) {
                super.insertString(fb, offset, string, attr);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text == null)
                return;
            if (isStringNumeric(text) && offset < maxColumns) {
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
