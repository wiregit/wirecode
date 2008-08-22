package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;

class ResizingInputPanel extends JPanel implements Displayable {
    private JTextArea text;
    private MessageWriter writer;

    public ResizingInputPanel(MessageWriter writer) {
        super(new BorderLayout());

        this.writer = writer;
        text = new JTextArea();
        text.setRows(2);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
        text.getActionMap().put("sendMessage", new SendMessage());
        text.getDocument().addDocumentListener(new TextAreaResizer());

        JScrollPane scrollPane = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane);
    }
    
    @Override
    public void handleDisplay() {
        text.requestFocusInWindow();
    }

    private class SendMessage extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String message = text.getText();
                if (message.trim().length() > 0) {
                    writer.writeMessage(message);
                    text.setText("");
                }
            } catch (XMPPException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    private class TextAreaResizer implements DocumentListener {
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
}
