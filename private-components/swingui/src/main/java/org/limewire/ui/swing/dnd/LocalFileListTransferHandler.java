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
import org.limewire.ui.swing.library.LibrarySupport;
import org.limewire.ui.swing.util.DNDUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

public abstract class LocalFileListTransferHandler extends TransferHandler {
    private final LibrarySupport librarySupport;

    public LocalFileListTransferHandler(LibrarySupport librarySupport) {
        this.librarySupport = librarySupport;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        LocalFileTransferable transferable = null;
        EventSelectionModel<LocalFileItem> selectionModel = getSelectionModel();
        if (selectionModel != null) {
            List<File> files = new ArrayList<File>();
            EventList<LocalFileItem> selected = selectionModel.getSelected();
            for (LocalFileItem fileItem : selected) {
                files.add(fileItem.getFile());
            }
            transferable = new LocalFileTransferable(files.toArray(new File[files.size()]));
        }
        return transferable;
    }

    public abstract EventSelectionModel<LocalFileItem> getSelectionModel();

    public abstract LocalFileList getLocalFileList();

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
        librarySupport.addFiles(localFileList, files);
    }
}
