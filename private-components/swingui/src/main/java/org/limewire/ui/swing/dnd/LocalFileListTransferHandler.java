package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorTable;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.util.DNDUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.Inject;

public class LocalFileListTransferHandler extends TransferHandler {

    private final LibraryNavigatorTable libraryNavigatorTable;
    private final LibraryTable libraryTable;

    @Inject
    public LocalFileListTransferHandler(LibraryNavigatorTable libraryNavigatorTable, 
            LibraryTable libraryTable) {
        this.libraryNavigatorTable = libraryNavigatorTable;
        this.libraryTable = libraryTable;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        LocalFileTransferable transferable = null;
        if (libraryTable.getSelectionModel() != null) {
            List<File> files = new ArrayList<File>();
            EventList<LocalFileItem> selected = ((EventSelectionModel<LocalFileItem>) libraryTable.getSelectionModel()).getSelected();
            for (LocalFileItem fileItem : selected) {
                files.add(fileItem.getFile());
            }
            transferable = new LocalFileTransferable(files.toArray(new File[files.size()]));
        }
        return transferable;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        if (getLocalFileList() == null || !DNDUtils.containsFileFlavors(info)) {
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

        LocalFileList localFileList = getLocalFileList();
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
        LocalFileList localFileList = getLocalFileList();
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
    
    private LocalFileList getLocalFileList() {
        LibraryNavItem item = libraryNavigatorTable.getSelectedItem();
        if(item == null)
            return null;
        return item.getLocalFileList();
    }

}
