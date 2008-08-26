package org.limewire.ui.swing.friends;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

public class CopyAction extends DefaultEditorKit.CopyAction {
    private final JTextComponent component;
    
    public CopyAction(JTextComponent component) {
        this.component = component;
    }
    
    @Override
    public boolean isEnabled() {
        return component.getSelectedText() != null;
    }
}
