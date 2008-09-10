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

public class ShareDropTarget implements DropTargetListener {

    private DropTarget dropTarget;
    private LocalFileList fileList;
    
    public ShareDropTarget(Component component, LocalFileList fileList) {
        dropTarget = new DropTarget(component, DnDConstants.ACTION_COPY, this, true, null);
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
                List filesList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                for(int i = 0; i < filesList.size(); i++) {
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

}
