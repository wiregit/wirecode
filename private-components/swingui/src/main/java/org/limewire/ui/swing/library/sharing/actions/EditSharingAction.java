package org.limewire.ui.swing.library.sharing.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.LibrarySharingPanel;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Switches the sharing view to the editable view.
 */
public class EditSharingAction extends AbstractAction {

    private final Provider<LibrarySharingPanel> librarySharingPanel;
    
    @Inject
    public EditSharingAction(Provider<LibrarySharingPanel> librarySharingPanel) {
        this.librarySharingPanel = librarySharingPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        librarySharingPanel.get().showEditableView();
    }
}
