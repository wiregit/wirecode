package org.limewire.ui.swing.library.navigator.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DeleteListAction extends AbstractAction {

    private final Provider<LibraryNavigatorTable> table;
    private final Provider<SharedFileListManager> sharedFileListManager;
    
    @Inject
    public DeleteListAction(Provider<LibraryNavigatorTable> table,
            Provider<SharedFileListManager> sharedFileListManager) {
        super(I18n.tr("Delete"));
        
        this.table = table;
        this.sharedFileListManager = sharedFileListManager;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        LibraryNavItem item = table.get().getSelectedItem();
        if(item != null && item.canRemove()) {
            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    //TODO: need remove API
//                    sharedFileListManager.get().
                }
            });
        }
    }
}
