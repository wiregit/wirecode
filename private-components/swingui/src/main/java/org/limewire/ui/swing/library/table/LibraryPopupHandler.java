package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Point;

import org.limewire.ui.swing.table.TablePopupHandler;

import com.google.inject.Provider;

class LibraryPopupHandler implements TablePopupHandler {
    private final LibraryTable libraryTable;
    private final Provider<LibraryPopupMenu> popupMenu;
    
    public LibraryPopupHandler(LibraryTable libraryTab, Provider<LibraryPopupMenu> popupMenu) {
        this.libraryTable = libraryTab;
        this.popupMenu = popupMenu;
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        int popupRow = libraryTable.rowAtPoint(new Point(x, y));
        libraryTable.setRowSelectionInterval(popupRow, popupRow);

        popupMenu.get().show(component, x, y);
    }
}
