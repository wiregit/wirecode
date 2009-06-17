package org.limewire.ui.swing.library.sharing.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.sharing.LibrarySharingEditablePanel;
import org.limewire.ui.swing.library.sharing.LibrarySharingPanel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ApplySharingAction extends AbstractAction {

    private final Provider<LibrarySharingPanel> librarySharingPanel;
    private final Provider<LibrarySharingEditablePanel> librarySharingEditablePanel;
    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    
    @Inject
    public ApplySharingAction(Provider<LibrarySharingPanel> librarySharingPanel,
            Provider<LibrarySharingEditablePanel> librarySharingEditablePanel,
            Provider<LibraryNavigatorPanel> libraryNavigatorPanel) {
        super(I18n.tr("Apply"));
        
        this.librarySharingPanel = librarySharingPanel;
        this.librarySharingEditablePanel = librarySharingEditablePanel;
        this.libraryNavigatorPanel = libraryNavigatorPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        List<String> friends = librarySharingEditablePanel.get().getSelectedFriendIds();
        librarySharingPanel.get().updateFriends(friends);
        libraryNavigatorPanel.get().repaint();
        librarySharingPanel.get().showNonEditableView();
    }
}
