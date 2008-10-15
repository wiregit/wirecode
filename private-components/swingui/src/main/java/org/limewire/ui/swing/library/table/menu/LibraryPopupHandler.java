package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.table.TablePopupHandler;

public class LibraryPopupHandler implements TablePopupHandler {
    private int popupRow = -1;

    private LibraryTable<LocalFileItem> table;

    private MyLibraryPopupMenu popupMenu;
    private MyLibraryMultipleSelectionPopupMenu multiSelectPopupMenu;

    public LibraryPopupHandler(LibraryTable<LocalFileItem> table, Category category, LibraryManager libraryManager,
            ShareListManager shareListManager, List<SharingTarget> friendList) {
        this.table = table;
        this.popupMenu = new MyLibraryPopupMenu(category, libraryManager, shareListManager, table, friendList);
        this.multiSelectPopupMenu = new MyLibraryMultipleSelectionPopupMenu(category, libraryManager, shareListManager, table, friendList);

    }

    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = table.rowAtPoint(new Point(x, y));
        List<LocalFileItem> items = table.getSelectedItems();        
        LocalFileItem selectedItem = table.getLibraryTableModel().getFileItem(popupRow);
        if(items.contains(selectedItem) && items.size() > 1) {
            multiSelectPopupMenu.setFileItems(items);
            multiSelectPopupMenu.show(component, x, y);
        } else {
            table.setRowSelectionInterval(popupRow, popupRow);
            popupMenu.setFileItem(selectedItem);
            popupMenu.show(component, x, y);
        }
    }
}
