package org.limewire.ui.swing.library.sharing;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class SelectAllAction extends AbstractAction {

    Provider<LibrarySharingEditablePanel> sharingEditablePanel;
    
    @Inject
    public SelectAllAction(Provider<LibrarySharingEditablePanel> sharingEditablePanel) {
        super(I18n.tr("all"));
        
        this.sharingEditablePanel = sharingEditablePanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        sharingEditablePanel.get().selectAllFriends();
    }
}
