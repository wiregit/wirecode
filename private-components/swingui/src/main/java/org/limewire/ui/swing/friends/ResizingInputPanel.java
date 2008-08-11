package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class ResizingInputPanel extends JPanel implements DocumentListener {
    private JTextArea text;
    
    public ResizingInputPanel() {
        super(new BorderLayout());
        
        text = new JTextArea();
        text.setRows(2);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.getDocument().addDocumentListener(this);
        
        JScrollPane scrollPane = new JScrollPane(text, 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // TODO Handle resize behavior for input box
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        // TODO Handle resize behavior for input box
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        // TODO Handle resize behavior for input box
    }
}
