package org.limewire.ui.swing.library.popup;

import javax.swing.JPopupMenu;

import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.library.navigator.actions.AddFilesAction;
import org.limewire.ui.swing.library.navigator.actions.ClearAction;
import org.limewire.ui.swing.library.navigator.actions.DeleteListAction;
import org.limewire.ui.swing.library.navigator.actions.ExportListAction;
import org.limewire.ui.swing.library.navigator.actions.ImportListAction;
import org.limewire.ui.swing.library.navigator.actions.RenameAction;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryNavPopupMenu extends JPopupMenu {

    private final AddFilesAction addAction;
    private final ImportListAction importAction;
    private final ExportListAction exportAction;
    private final Provider<RenameAction> renameAction;
    private final ClearAction clearAction;
    private final Provider<DeleteListAction> deleteAction;
    
    @Inject
    public LibraryNavPopupMenu(LibraryNavigatorTable table, 
            AddFilesAction addAction, ImportListAction importAction,
            ExportListAction exportAction, Provider<RenameAction> renameAction,
            ClearAction clearAction, Provider<DeleteListAction> deleteAction) {
        this.addAction = addAction;
        this.importAction = importAction;
        this.exportAction = exportAction;
        this.renameAction = renameAction;
        this.clearAction = clearAction;
        this.deleteAction = deleteAction;

        init(table.getSelectedItem());
    }
    
    public void init(LibraryNavItem item) {
        
        add(addAction);
        addSeparator();
        add(importAction);
        add(exportAction);
        addSeparator();
        if(item.getType() == NavType.LIBRARY || item.getType() == NavType.PUBLIC_SHARED) {
            add(clearAction);
        } else {
            add(renameAction.get());
            addSeparator();
            add(clearAction);
            add(deleteAction.get()).setEnabled(item.canRemove());
        }
    }
}
