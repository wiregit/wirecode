package org.limewire.ui.swing.dnd;

import java.awt.Point;

import javax.swing.TransferHandler;

import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;

public class LibraryNavTransferHandler extends TransferHandler {

    private final LocalFileListTransferHandler localFileListTransferHandler = new LocalFileListTransferHandler();

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
        localFileListTransferHandler.setFileList(null, libraryNavItem.getLocalFileList());
        return localFileListTransferHandler.canImport(info);
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
        localFileListTransferHandler.setFileList(null, libraryNavItem.getLocalFileList());
        return localFileListTransferHandler.importData(info);
    }
}
