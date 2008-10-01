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
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

/**
 * A transfer handler for adding shared files to a table.
 */
public class SharingTransferHandler extends TransferHandler {

    private final LocalFileList fileList;

    private final boolean alwaysShareDocuments;

    public SharingTransferHandler(LocalFileList fileList) {
        this(fileList, false);
    }

    public SharingTransferHandler(LocalFileList fileList, boolean alwaysShareDocuments) {
        this.fileList = fileList;
        this.alwaysShareDocuments = alwaysShareDocuments;
    }

    /**
     * Currently only support a list of Files to be imported to this component
     */
    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        if (!DNDUtils.containsFileFlavors(info.getDataFlavors())) {
            return false;
        }
        return true;
    }

    /**
     * Performs the actual importing of the files. Note the files are not
     * directly added to the Model but rather added to the FileMananager first.
     */
    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if (!info.isDrop())
            return false;

        Transferable transferable = info.getTransferable();

        try {
            handleDrop(transferable, fileList);
        } catch (UnsupportedFlavorException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private void handleDrop(Transferable transferable, LocalFileList fileList) throws UnsupportedFlavorException, IOException {
        final LocalFileList currentModel = fileList;
        final File[] droppedFiles = DNDUtils.getFiles(transferable);

        final List<File> acceptedFiles = new ArrayList<File>();

        // perform the file IO operations on its own thread.
        // TODO: give feedback for failed adds.\
        //TODO this code is duplicated from ShareDropTarget -- need to refactor both classes
        BackgroundExecutorService.schedule(new Runnable() {
            public void run() {
                for (int i = 0; i < droppedFiles.length; i++) {
                    File file = droppedFiles[i];
                    if (file == null)
                        continue;
                    if (file.isDirectory()) {
                        readDirectory(file, acceptedFiles);
                    } else if (isAllowed(FileUtils.getFileExtension(file.getName()))) {
                        acceptedFiles.add(file);
                    }
                }
                for (File file : acceptedFiles) {
                    currentModel.addFile(file);
                }
            }
        });
    }

    /**
     * Reads files in a directory. Subdirectories are ignored and any allowed
     * file within this directory is added to the accept list.
     */
    private void readDirectory(File directory, List filesList) {
        for (File file : directory.listFiles()) {
            if (!file.isDirectory() && isAllowed(FileUtils.getFileExtension(file.getName()))) {
                fileList.addFile(file);
            }
        }
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
}
