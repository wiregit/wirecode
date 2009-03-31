package org.limewire.ui.swing.library.table;

import javax.swing.Action;


public interface ShareTableRendererEditorFactory {

    public ShareTableRendererEditor createShareTableRendererEditor(Action friendAction, Action p2pAction);
    
}