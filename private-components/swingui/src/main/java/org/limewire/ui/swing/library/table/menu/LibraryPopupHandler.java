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

    private LibraryTable table;

    private MyLibraryPopupMenu popupMenu;
    private MyLibraryMultipleSelectionPopupMenu multiSelectPopupMenu;

    public LibraryPopupHandler(LibraryTable table, Category category, LibraryManager libraryManager,
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
        int[] selectedRows = table.getSelectedRows();
        if (valueInArray(popupRow, selectedRows) && selectedRows.length > 1) {
            LocalFileItem[] fileItems = new LocalFileItem[selectedRows.length];
            for (int i = 0; i < fileItems.length; i++) {
                fileItems[i] = (LocalFileItem) table.getLibraryTableModel().getFileItem(selectedRows[i]);
                multiSelectPopupMenu.setFileItems(fileItems);
            }
            multiSelectPopupMenu.show(component, x, y);
        } else {
            table.setRowSelectionInterval(popupRow, popupRow);
            popupMenu.setFileItem((LocalFileItem) table.getLibraryTableModel().getFileItem(popupRow));
            popupMenu.show(component, x, y);
        }

    }
    
    private boolean valueInArray(int value, int[] array){
        for (int arrayValue : array){
            if (arrayValue == value){
                return true;
            }
        }
        return false;
    }

}
