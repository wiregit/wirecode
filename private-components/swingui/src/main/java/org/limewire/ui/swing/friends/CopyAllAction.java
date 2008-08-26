package org.limewire.ui.swing.friends;

import javax.swing.text.DefaultEditorKit;

class CopyAllAction extends AbstractTextAction {
    
    public CopyAllAction() {
        super("Copy All", 
              DefaultEditorKit.selectAllAction, 
              DefaultEditorKit.copyAction);
    }
}
