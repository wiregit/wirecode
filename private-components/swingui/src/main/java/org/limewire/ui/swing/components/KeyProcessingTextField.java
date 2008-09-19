package org.limewire.ui.swing.components;

import java.awt.event.KeyEvent;

import javax.swing.text.Document;

public class KeyProcessingTextField extends ClipboardEnabledTextField {
    
    public KeyProcessingTextField() {
        super();
    }
    
    public KeyProcessingTextField(String text) {
        super(text);
    }
    
    public KeyProcessingTextField(int columns) {
        super(columns);
    }
    
    public KeyProcessingTextField(String text, int columns) {
        super(text, columns);
    }
    
    public KeyProcessingTextField(Document doc, String text, int columns) {
        super(doc, text, columns);
    }
    
    // raise access
    @Override
    public void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
    }
}