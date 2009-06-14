package org.limewire.ui.swing.library.navigator.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RenameAction extends AbstractAction {

    private final Provider<LibraryNavigatorPanel> libraryNavigatorPanel;
    private final Provider<SharedFileListManager> sharedFileListManager;
    
    @Inject
    public RenameAction(Provider<LibraryNavigatorPanel> libraryNavigatorPanel,
            Provider<SharedFileListManager> sharedFileListManager) {
        super(I18n.tr("Rename"));
        
        this.libraryNavigatorPanel = libraryNavigatorPanel;
        this.sharedFileListManager = sharedFileListManager;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        throw new NotImplementedException("not implemented yet");
        //TODO: need to create way to edit name
//        final LibraryNavItem item = table.get().getSelectedItem();
//        if(item != null && item.canRemove()) {
//            BackgroundExecutorService.execute(new Runnable() {
//                @Override
//                public void run() {
//                    sharedFileListManager.get().deleteSharedFileList(item.getTabID());
//                }
//            });
//        }
    }
}
