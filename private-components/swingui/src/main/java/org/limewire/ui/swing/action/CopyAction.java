package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

public class CopyAction extends AbstractSelectionRequiredTextAction {
    
    public CopyAction(JTextComponent component) {
        super("Copy", component, DefaultEditorKit.copyAction);
    }
}
