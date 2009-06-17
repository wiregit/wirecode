package org.limewire.ui.swing.dnd;

import java.awt.Point;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;

import ca.odell.glazedlists.swing.EventSelectionModel;

public class LibraryNavTransferHandler extends LocalFileListTransferHandler {
    
    private LocalFileList localFileList = null;
    @Override
    public boolean canImport(TransferSupport info) {

        if (!LibraryNavigatorTable.class.isInstance(info.getComponent())) {
            return false;
        }

        LibraryNavigatorTable libraryNavigatorTable = (LibraryNavigatorTable) info.getComponent();
        DropLocation dropLocation = info.getDropLocation();
        Point point = dropLocation.getDropPoint();
        int column = libraryNavigatorTable.columnAtPoint(point);
        int row = libraryNavigatorTable.rowAtPoint(point);

        LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.getValueAt(row,
                column);
        if (libraryNavItem == null) {
            return false;
        }
        this.localFileList = libraryNavItem.getLocalFileList();
        
        return super.canImport(info);
    }

    @Override
    public boolean importData(TransferSupport info) {
        if (!LibraryNavigatorTable.class.isInstance(info.getComponent())) {
            return false;
        }

        LibraryNavigatorTable libraryNavigatorTable = (LibraryNavigatorTable) info.getComponent();

        DropLocation dropLocation = info.getDropLocation();
        Point point = dropLocation.getDropPoint();
        int column = libraryNavigatorTable.columnAtPoint(point);
        int row = libraryNavigatorTable.rowAtPoint(point);
        LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.getValueAt(row,
                column);
        if (libraryNavItem == null) {
            return false;
        }
        this.localFileList = libraryNavItem.getLocalFileList();
        
        return super.importData(info);
    }

    @Override
    public LocalFileList getLocalFileList() {
        return localFileList;
    }

    @Override
    public EventSelectionModel<LocalFileItem> getSelectionModel() {
        return null;
    }

}
