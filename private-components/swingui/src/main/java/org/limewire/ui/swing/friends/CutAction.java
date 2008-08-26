package org.limewire.ui.swing.friends;

import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

public class CutAction extends AbstractSelectionRequiredTextAction {

    public CutAction(JTextComponent component) {
        super("Cut", component, DefaultEditorKit.cutAction);
    }
}
