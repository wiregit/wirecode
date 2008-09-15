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
import java.util.List;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

public class ShareDropTarget implements DropTargetListener {

    private DropTarget dropTarget;
    private LocalFileList fileList;
    private boolean alwaysShareDocuments;
    
    public ShareDropTarget(Component component, LocalFileList fileList) {
        this(component, fileList, true);
    }
    
    public ShareDropTarget(Component component, LocalFileList fileList, boolean alwaysShareDocuments) {
        dropTarget = new DropTarget(component, DnDConstants.ACTION_COPY, this, true, null);
        this.fileList = fileList;
        this.alwaysShareDocuments = alwaysShareDocuments;
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
                List filesList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for(int i = 0; i < filesList.size(); i++) {
                    if(isAllowed(FileUtils.getFileExtension( ((File)filesList.get(i)).getName())))               
                        fileList.addFile((File)filesList.get(i));
                }
                dtde.dropComplete(true);
            } catch (Exception e) { 
            	dtde.dropComplete(false);
            }
          } else { 
          		dtde.rejectDrop();
          }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }
    
    //TODO: this does't provide feedback to the user as to when documents/programs were
    //  rejected
    private boolean isAllowed(String fileName) {
        if(!alwaysShareDocuments && !SharingSettings.DOCUMENT_SHARING_ENABLED.getValue()) {
            MediaType type = MediaType.getMediaTypeForExtension(fileName);
            if(type.getMimeType().equals(MediaType.SCHEMA_DOCUMENTS)) {
                return false;
            }
        }
        if(!SharingSettings.PROGRAM_SHARING_ENABLED.getValue()) {
            MediaType type = MediaType.getMediaTypeForExtension(fileName);
            if(type.getMimeType().equals(MediaType.SCHEMA_PROGRAMS))
                return false;
        }
        return true;
    }

}
