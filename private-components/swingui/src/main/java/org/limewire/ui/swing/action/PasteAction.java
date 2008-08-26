package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;

public class PasteAction extends AbstractTextAction {

    public PasteAction() {
        super("Paste", DefaultEditorKit.pasteAction);
    }
}
