package org.limewire.ui.swing.sharing.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.LibraryManager;

public class GoToLibraryAction extends AbstractAction {
    
    private LibraryManager libraryManager;
    
    public GoToLibraryAction(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        if(libraryManager.containsBuddyLibrary("")) {
//            //TODO: do something here
//        }
    }
}
