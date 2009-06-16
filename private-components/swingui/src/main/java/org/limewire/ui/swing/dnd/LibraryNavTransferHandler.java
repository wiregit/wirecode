package org.limewire.ui.swing.dnd;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.util.DNDUtils;

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
        localFileListTransferHandler.setFileList(libraryNavItem.getLocalFileList());
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
        localFileListTransferHandler.setFileList(libraryNavItem.getLocalFileList());
        return localFileListTransferHandler.importData(info);
    }
    
    private class LocalFileListTransferHandler extends TransferHandler {

        private LocalFileList localFileList;
        
        public LocalFileListTransferHandler() {
        }

        public void setFileList(LocalFileList localFileList) {
            this.localFileList = localFileList;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            return null;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            if (localFileList == null || !DNDUtils.containsFileFlavors(info)) {
                return false;
            }

            List<File> files = Collections.emptyList();
            if (DNDUtils.containsFileFlavors(info)) {
                Transferable t = info.getTransferable();
                try {
                    files = Arrays.asList(DNDUtils.getFiles(t));
                } catch (Throwable failed) {
                    return true;
                }
            }

            for (File file : files) {
                if (localFileList.isFileAddable(file)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            List<File> files = Collections.emptyList();
            if (DNDUtils.containsFileFlavors(info)) {
                Transferable t = info.getTransferable();
                try {
                    files = Arrays.asList(DNDUtils.getFiles(t));
                } catch (Throwable failed) {
                    return false;
                }
            }

            handleFiles(files);
            return true;
        }

        private void handleFiles(final List<File> files) {
            for (File file : files) {
                if (localFileList.isFileAddable(file)) {
                    if (file.isDirectory()) {
                        localFileList.addFolder(file);
                    } else {
                        localFileList.addFile(file);
                    }
                }
            }
        }
    }

}
