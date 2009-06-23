package org.limewire.ui.swing.dnd;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.LibrarySupport;
import org.limewire.ui.swing.util.DNDUtils;

/**
 * An abstract class for transferring {@link LocalFileItem} through a
 * {@link TransferHandler}.
 */
public abstract class LocalFileListTransferHandler extends TransferHandler {
    private final WeakHashMap<Transferable, Map<LocalFileList, Boolean>> canImportCache = new WeakHashMap<Transferable, Map<LocalFileList, Boolean>>();

    private final LibrarySupport librarySupport;

    public LocalFileListTransferHandler(LibrarySupport librarySupport) {
        this.librarySupport = librarySupport;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        List<File> files = getSelectedFiles();
        if (!files.isEmpty()) {
            return new LocalFileTransferable(files.toArray(new File[files.size()]));
        } else {
            return null;
        }
    }

    /** Returns all files that want to be transferred. */
    protected abstract List<File> getSelectedFiles();

    /** Returns the LocalFileList that items should be transfered to or from. */
    protected abstract LocalFileList getLocalFileList();

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        Transferable t = info.getTransferable();
        LocalFileList localFileList = getLocalFileList();
        Map<LocalFileList, Boolean> canImportMap = canImportCache.get(t);
        if (canImportMap == null) {
            canImportMap = new HashMap<LocalFileList, Boolean>();
            canImportCache.put(t, canImportMap);
        }

        Boolean canImport = canImportMap.get(localFileList);
        if (canImport == null) {
            canImport = canImportInternal(info);
            canImportMap.put(localFileList, canImport);
        }
        
        return canImport;
    }

    private boolean canImportInternal(TransferHandler.TransferSupport info) {
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

        Transferable t = info.getTransferable();
        canImportCache.remove(t);

        List<File> files = Collections.emptyList();
        if (DNDUtils.containsFileFlavors(info)) {
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
