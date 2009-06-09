package org.limewire.ui.swing.library.popup;

import javax.swing.JPopupMenu;

import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.library.navigator.actions.DeleteListAction;

import com.google.inject.Inject;

public class LibraryNavPopupMenu extends JPopupMenu {

    private DeleteListAction deleteAction;
    
    @Inject
    public LibraryNavPopupMenu(LibraryNavigatorTable table, DeleteListAction deleteAction) {
        this.deleteAction = deleteAction;
        
        //        add(new JMenuItem("fake menu Item"));
//        add(new JMenuItem("fake menu item 2"));
        init(table.getSelectedItem());
    }
    
    public void init(LibraryNavItem item) {
        add(deleteAction).setEnabled(item.canRemove());
    }
}
