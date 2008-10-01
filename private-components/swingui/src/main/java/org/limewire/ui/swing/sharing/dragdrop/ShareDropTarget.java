package org.limewire.ui.swing.sharing.dragdrop;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

/**
 * A drop target for adding a drop listener to a component. The drop listener
 * is specialized for adding files to shared lists. Unlike the library, Programs
 * and Documents may be rejected based on the user settings. 
 * 
 * This drop target accepts a list of files or directories. Subdirectories within
 * a folder drop are ignored. If a drop contains multiple file types or a folder
 * with multiple subfile types, the drop is still accepted if it contains programs
 * or documents. Only those files of acceptable type are accepted.
 * 
 * Currently no feedback is displayed if one or more files are not added to the list
 * depsite the drop being accepted.
 */
//TODO: users recieve no feedback when program or documents files are dropped and rejected.
public class ShareDropTarget implements DropTargetListener {

    private DropTarget dropTarget;
    private LocalFileList fileList;
    
    /**
     * Boolean value to ignore the document sharing setting in SharingSettings.
     * If true, SharingSettings.DOCUMENT_SHARING_ENABLED value is ignored, if
     * false, SharingSettings.DOCUMENT_SHARING_ENABLED value is  heeded.
     */
    private boolean alwaysShareDocuments;
    
    public ShareDropTarget(Component component, LocalFileList fileList) {
        this(component, fileList, true);
    }
    
    public ShareDropTarget(Component component, LocalFileList fileList, boolean alwaysShareDocuments) {
        dropTarget = new DropTarget(component, DnDConstants.ACTION_COPY, this, true, null);
        this.fileList = fileList;
        this.alwaysShareDocuments = alwaysShareDocuments;
    }
    
    public void setModel(LocalFileList fileList) {
        this.fileList = fileList;
    }
    
    public DropTarget getDropTarget() {
        return dropTarget;
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        if ((dtde.getDropAction() & DnDConstants.ACTION_COPY_OR_MOVE) != 0) {
            // Accept the drop and get the transfer data
            dtde.acceptDrop(dtde.getDropAction());
            Transferable transferable = dtde.getTransferable();

            try {
                final LocalFileList currentModel = fileList;
                final List droppedFiles = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor); 
                
                final List<File> acceptedFiles = new ArrayList<File>();
          
                // perform the file IO operations on its own thread. 
                //TODO: give feedback for failed adds.
                BackgroundExecutorService.schedule(new Runnable(){
                    public void run() {
                        for(int i = droppedFiles.size()-1; i >= 0; i--) { 
                            File file = (File)droppedFiles.get(i);
                            if(file == null)
                                continue;
                            if(file.isDirectory()) {
                                readDirectory(file, acceptedFiles);
                            }
                            else if(isAllowed(FileUtils.getFileExtension(file.getName()))) { 
                                acceptedFiles.add(file);
                            }
                        }

                        for(File file : acceptedFiles) {       
                            currentModel.addFile(file);
                        }
                        dropCompleted();
                    }
                });

                dtde.dropComplete(true);
            } catch (Exception e) {
            	dtde.dropComplete(false);
            }
          } else {
          		dtde.rejectDrop();
          }
    }
    
    /**
     * Notification method for subclasses to indicate that all
     * files accepted by the drop have been added to the model.
     */
    protected void dropCompleted() {
        //no-op
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }
    
    /**
     * Reads files in a directory. Subdirectories are ignored and any allowed file
     * within this directory is added to the accept list.
     */
    private void readDirectory(File directory, List filesList) {
        for(File file : directory.listFiles()) {
            if(!file.isDirectory() && isAllowed(FileUtils.getFileExtension(file.getName()))) {
                fileList.addFile(file);
            }
        }
    }
    
    /**
     * Tests whether the file is droppable. If the file is of type Program or Document,
     * the validity is based on SharingSettings, otherwise the file is accepted.
     * 
     * @param fileExtension - the file extension to test
     * @return true if the file is allowed, false otherwise.
     */
    private boolean isAllowed(String fileExtension) {
        if(fileExtension != null) {        
            if(!alwaysShareDocuments && !SharingSettings.DOCUMENT_SHARING_ENABLED.getValue()) {
                MediaType type = MediaType.getMediaTypeForExtension(fileExtension);
                if(type == null || type.getSchema().equals(MediaType.SCHEMA_DOCUMENTS)) {
                    return false;
                }
            }
            if(!SharingSettings.PROGRAM_SHARING_ENABLED.getValue()) {
                MediaType type = MediaType.getMediaTypeForExtension(fileExtension);
                if(type == null || type.getSchema().equals(MediaType.SCHEMA_PROGRAMS))
                    return false;
            }
        }
        return true;
    }

}
