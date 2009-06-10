package org.limewire.ui.swing.library.sharing.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.LibrarySharingEditablePanel;
import org.limewire.ui.swing.library.sharing.LibrarySharingPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ApplySharingAction extends AbstractAction {

    private final Provider<LibrarySharingPanel> librarySharingPanel;
    private final Provider<LibrarySharingEditablePanel> librarySharingEditablePanel;
    
    @Inject
    public ApplySharingAction(Provider<LibrarySharingPanel> librarySharingPanel,
            Provider<LibrarySharingEditablePanel> librarySharingEditablePanel) {
        super(I18n.tr("Apply"));
        
        this.librarySharingPanel = librarySharingPanel;
        this.librarySharingEditablePanel = librarySharingEditablePanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        List<String> friends = librarySharingEditablePanel.get().getSelectedFriends();
        librarySharingPanel.get().updateFriends(friends);
        librarySharingPanel.get().showNonEditableView();
    }
}
