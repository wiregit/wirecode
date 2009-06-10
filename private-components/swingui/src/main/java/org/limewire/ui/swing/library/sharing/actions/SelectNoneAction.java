package org.limewire.ui.swing.library.sharing.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.LibrarySharingEditablePanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SelectNoneAction extends AbstractAction {

    Provider<LibrarySharingEditablePanel> sharingEditablePanel;
    
    @Inject
    public SelectNoneAction(Provider<LibrarySharingEditablePanel> sharingEditablePanel) {
        super(I18n.tr("none"));
        
        this.sharingEditablePanel = sharingEditablePanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        sharingEditablePanel.get().deselectAllFriends();
    }
}
