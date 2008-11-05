package org.limewire.ui.swing.sharing.dragdrop;

import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.util.DNDUtils;

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

    private final DropTarget dropTarget;
    private LocalFileList fileList;
    
    public ShareDropTarget(Component component, LocalFileList fileList) {
        dropTarget = new DropTarget(component, DnDConstants.ACTION_COPY, this, true, null);
        this.fileList = fileList;
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
                final File[] droppedFiles = DNDUtils.getFiles(transferable); 
          
                for(File file : droppedFiles) {
                    if(file != null) {
                        if(file.isDirectory()) {
                            currentModel.addFolder(file);
                        } else {
                            currentModel.addFile(file);
                        }
                        acceptedFile(file);
                    }
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
    
    /** A hook for subclasses that want to perform processing on all files that were accepted. */
    protected void acceptedFile(File files) {
        // no-op by default.
    }
}
