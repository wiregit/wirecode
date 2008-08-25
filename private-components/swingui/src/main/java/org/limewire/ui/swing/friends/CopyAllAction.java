package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

class CopyAllAction extends DefaultEditorKit.CopyAction {

    public void actionPerformed(ActionEvent e) {
        JTextComponent target = getTextComponent(e);
        if (target != null) {
            Document doc = target.getDocument();
            target.setCaretPosition(0);
            target.moveCaretPosition(doc.getLength());
        }
        
        super.actionPerformed(e);
    }
}
