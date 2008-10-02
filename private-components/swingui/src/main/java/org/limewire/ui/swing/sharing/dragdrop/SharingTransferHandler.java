package org.limewire.ui.swing.sharing.dragdrop;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

/**
 * A transfer handler for adding shared files to a table.
 */
public class SharingTransferHandler extends TransferHandler {

    private static final Log LOG = LogFactory.getLog(SharingTransferHandler.class);

    private LocalFileList fileList;

    private final boolean alwaysShareDocuments;

    /**
     * This variable is used to track the status of the files being dragged.
     * During a drag the Transferable object is available in the canImport
     * method, however when canImport is call just prior to a drop the
     * Transferable object is not available and throws a
     * InvalidDnDOperationException. The work around for that error case is to
     * track the drags and store if the last drag was good. If it was and we get
     * that exception, assume that canImport should return true.
     */
    private boolean lastDragOk = false;

    public SharingTransferHandler(LocalFileList fileList) {
        this(fileList, false);
    }

    public SharingTransferHandler(LocalFileList fileList, boolean alwaysShareDocuments) {
        this.fileList = fileList;
        this.alwaysShareDocuments = alwaysShareDocuments;

    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        if (!DNDUtils.containsFileFlavors(info)) {
            return false;
        }
        return true;
//
//        try {
//            Transferable transferable = info.getTransferable();
//            transferable.getTransferData(DNDUtils.URIFlavor);
//            final File[] droppedFiles = DNDUtils.getFiles(transferable);
//            for (int i = 0; i < droppedFiles.length; i++) {
//                File fileOrDirectory = droppedFiles[i];
//                if (hasValidFiles(fileOrDirectory)) {
//                    LOG.debug("DND valid file found.");
//                    lastDragOk = true;
//                    return true;
//                }
//            }
//            LOG.debug("DND valid file not found.");
//            lastDragOk = false;
//        } catch (java.awt.dnd.InvalidDnDOperationException e) {
//            LOG.debugf("Transferable not available, check lastDrag: {0}", Boolean
//                    .valueOf(lastDragOk), e);
//            return lastDragOk;
//        } catch (UnsupportedFlavorException e) {
//            LOG.debug(e.getMessage(), e);
//            return false;
//        } catch (IOException e) {
//            LOG.debug(e.getMessage(), e);
//            return false;
//        }
//        return false;
    }

    /**
     * Performs the actual importing of the files. Note the files are not
     * directly added to the Model but rather added to the FileMananager first.
     */
    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop()) {
            return false;
        }
        
        Transferable transferable = info.getTransferable();

        try {
            return handleDrop(transferable, fileList);
        } catch (UnsupportedFlavorException e) {
            LOG.debug(e.getMessage(), e);
            return false;
        } catch (IOException e) {
            LOG.debug(e.getMessage(), e);
            return false;
        }
    }

    private boolean handleDrop(Transferable transferable, LocalFileList fileList)
            throws UnsupportedFlavorException, IOException {
        final LocalFileList currentModel = fileList;
        final File[] droppedFiles = DNDUtils.getFiles(transferable);
        final List<File> acceptedFiles = getAllowedFiles(droppedFiles);
        // perform the file IO operations on its own thread.
        // TODO: give feedback for failed adds.\
        // TODO this code is duplicated from ShareDropTarget -- need to refactor
        // both classes
        BackgroundExecutorService.schedule(new Runnable() {
            public void run() {
                for (File file : acceptedFiles) {
                    currentModel.addFile(file);
                }
            }

        });
        return acceptedFiles.size() > 0;
    }

    /**
     * Returns a list of files that pass the isAllowed method.
     */
    private List<File> getAllowedFiles(final File[] droppedFiles) {
        List<File> acceptedFiles = new ArrayList<File>();

        for (int i = 0; i < droppedFiles.length; i++) {
            File file = droppedFiles[i];
            if (file == null)
                continue;

            readFileOrDirectory(file, acceptedFiles);
        }

        return acceptedFiles;
    }

    /**
     * Reads files in a directory. Subdirectories are ignored and any allowed
     * file within this directory is added to the accept list.
     */
    private void readFileOrDirectory(File fileOrDirectory, List<File> filesList) {
        checkAddFile(fileOrDirectory, filesList);
        if (fileOrDirectory.isDirectory()) {
            for (File file : fileOrDirectory.listFiles()) {
                checkAddFile(file, filesList);
            }
        }
    }

    /**
     * Checks if the given file or directory has any valid files.
     */
    private boolean hasValidFiles(File fileOrDirectory) {
        if (isAllowed(fileOrDirectory)) {
            return true;
        }

        if (fileOrDirectory.isDirectory()) {
            for (File file : fileOrDirectory.listFiles()) {
                if (isAllowed(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks a file against the isAllowed method. If it passes it is added to
     * fileList.
     */
    private void checkAddFile(File file, List<File> filesList) {
        if (isAllowed(file)) {
            filesList.add(file);
        }
    }

    /**
     * Checks if the given file is allowed. Delegating to isAllowed(String
     * fileExtension).
     */
    private boolean isAllowed(File file) {
        return !file.isDirectory() && isAllowed(FileUtils.getFileExtension(file.getName()));
    }

    /**
     * Tests whether the file is droppable. If the file is of type Program or
     * Document, the validity is based on SharingSettings, otherwise the file is
     * accepted.
     * 
     * @param fileExtension - the file extension to test
     * @return true if the file is allowed, false otherwise.
     */
    private boolean isAllowed(String fileExtension) {
        if (!fileExtension.isEmpty()) {
            if (!alwaysShareDocuments && !SharingSettings.DOCUMENT_SHARING_ENABLED.getValue()) {
                MediaType type = MediaType.getMediaTypeForExtension(fileExtension);
                if (type == null || type.getSchema().equals(MediaType.SCHEMA_DOCUMENTS)) {
                    return false;
                }
            }
            if (!SharingSettings.PROGRAM_SHARING_ENABLED.getValue()) {
                MediaType type = MediaType.getMediaTypeForExtension(fileExtension);
                if (type == null || type.getSchema().equals(MediaType.SCHEMA_PROGRAMS))
                    return false;
            }
        }
        return true;
    }

    /**
     * Updates the LocalFileList backing this transferHandler.
     */
    public void setModel(LocalFileList fileList) {
        this.fileList = fileList;
    }
}
