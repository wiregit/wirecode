package org.limewire.ui.swing.library.navigator;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Creates a new PlayList with the name "Untitled", selects
 * that PlayList and then enables editing on it.
 */
class CreateListAction extends AbstractAction {

    private final Provider<SharedFileListManager> shareManager;
    private final Provider<LibraryNavigatorTable> navTable;
    
    @Inject
    public CreateListAction(Provider<SharedFileListManager> shareManager,
            Provider<LibraryNavigatorTable> navTable) {
        this.shareManager = shareManager;
        this.navTable = navTable;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final int id = shareManager.get().createNewSharedFileList(I18n.tr("Untitled"));
        navTable.get().selectLibraryNavItem(id);
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                navTable.get().setEditable(true);
                navTable.get().editCellAt(navTable.get().getSelectedRow(), 0);                
            }
        });
    }
}
