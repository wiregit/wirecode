package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.ShareListManager;

public class GoToLibraryAction extends AbstractAction {
    
    private ShareListManager libraryManager;
    
    public GoToLibraryAction(ShareListManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        if(libraryManager.containsBuddyLibrary("")) {
//            //TODO: do something here
//        }
    }
}
