package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class DeleteListAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    private final Provider<SharedFileListManager> sharedFileListManager;
    
    @Inject
    public DeleteListAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel,
            Provider<SharedFileListManager> sharedFileListManager) {
        super(I18n.tr("Delete"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
        this.sharedFileListManager = sharedFileListManager;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final LibraryNavItem item = libraryNavigatorPanel.get().getSelectedNavItem();
        sharedFileListManager.get().deleteSharedFileList((SharedFileList)item.getLocalFileList());
    }
}
