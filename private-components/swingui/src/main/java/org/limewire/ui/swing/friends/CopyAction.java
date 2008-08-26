package org.limewire.ui.swing.friends;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

class CopyAction extends AbstractSelectionRequiredTextAction {
    
    public CopyAction(JTextComponent component) {
        super("Copy", component, DefaultEditorKit.copyAction);
    }
}
