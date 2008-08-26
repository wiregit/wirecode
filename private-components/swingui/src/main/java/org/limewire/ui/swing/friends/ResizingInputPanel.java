package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
        
        JPopupMenu popup = PopupUtil.addPopupMenus(text, new CutAction(text), new CopyAction(text), 
                new PasteAction(), new DeleteAction(text));
        popup.addSeparator();
        popup.add(new SelectAllAction());

        final JScrollPane scrollPane = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (text.getText().length() > 0 && scrollPane.getVerticalScrollBar().isVisible()) {
                    resizeTextBox(3);
                }
            }
        });
        add(scrollPane);
    }
    
    private void resizeTextBox(int rowCount) {
        if (text.getRows() != rowCount) {
            text.setRows(rowCount);
            revalidate();
        }
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
                    resizeTextBox(2);
                }
            } catch (XMPPException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }
}
