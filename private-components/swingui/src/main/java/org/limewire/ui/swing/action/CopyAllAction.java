package org.limewire.ui.swing.action;

import javax.swing.text.DefaultEditorKit;

public class CopyAllAction extends AbstractTextAction {
    
    public CopyAllAction() {
        super("Copy All", 
              DefaultEditorKit.selectAllAction, 
              DefaultEditorKit.copyAction);
    }
}
