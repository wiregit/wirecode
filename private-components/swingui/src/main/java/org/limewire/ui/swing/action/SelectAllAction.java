package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;

public class SelectAllAction extends AbstractTextAction {

    public SelectAllAction() {
        super("Select All", DefaultEditorKit.selectAllAction);
    }
}
