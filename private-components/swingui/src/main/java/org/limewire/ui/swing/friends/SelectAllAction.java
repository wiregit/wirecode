package org.limewire.ui.swing.friends;

import javax.swing.text.DefaultEditorKit;

class SelectAllAction extends AbstractTextAction {

    public SelectAllAction() {
        super("Select All", DefaultEditorKit.selectAllAction);
    }
}
