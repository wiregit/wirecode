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
import org.limewire.ui.swing.util.DNDUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

public class LocalFileListTransferHandler extends TransferHandler {

    private LocalFileList fileList;

    private EventSelectionModel<LocalFileItem> selectionModel;

    public LocalFileListTransferHandler() {
    }

    public void setFileList(EventSelectionModel<LocalFileItem> selectionModel,
            LocalFileList fileList) {
        this.fileList = fileList;
        this.selectionModel = selectionModel;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        LocalFileTransferable transferable = null;
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

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        return fileList != null && DNDUtils.containsFileFlavors(info);
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
            if (file.isDirectory()) {
                fileList.addFolder(file);
            } else {
                fileList.addFile(file);
            }
        }
    }

}
