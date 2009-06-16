package org.limewire.ui.swing.dnd;

import java.awt.Point;

import javax.swing.TransferHandler;

import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryNavTransferHandler extends TransferHandler {

    private final Provider<LibraryNavigatorTable> libraryNavigatorTable;

    private final LocalFileListTransferHandler localFileListTransferHandler = new LocalFileListTransferHandler();

    @Inject
    public LibraryNavTransferHandler(Provider<LibraryNavigatorTable> libraryNavigatorTable) {
        this.libraryNavigatorTable = libraryNavigatorTable;
    }

    @Override
    public boolean canImport(TransferSupport info) {
        DropLocation dropLocation = info.getDropLocation();
        Point point = dropLocation.getDropPoint();
        int column = libraryNavigatorTable.get().columnAtPoint(point);
        int row = libraryNavigatorTable.get().rowAtPoint(point);

        LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.get().getValueAt(
                row, column);
        if (libraryNavItem == null) {
            return false;
        }
        localFileListTransferHandler.setFileList(null, libraryNavItem.getLocalFileList());
        return localFileListTransferHandler.canImport(info);
    }

    @Override
    public boolean importData(TransferSupport info) {
        DropLocation dropLocation = info.getDropLocation();
        Point point = dropLocation.getDropPoint();
        int column = libraryNavigatorTable.get().columnAtPoint(point);
        int row = libraryNavigatorTable.get().rowAtPoint(point);
        LibraryNavItem libraryNavItem = (LibraryNavItem) libraryNavigatorTable.get().getValueAt(
                row, column);
        if (libraryNavItem == null) {
            return false;
        }
        localFileListTransferHandler.setFileList(null, libraryNavItem.getLocalFileList());
        return localFileListTransferHandler.importData(info);
    }
}
