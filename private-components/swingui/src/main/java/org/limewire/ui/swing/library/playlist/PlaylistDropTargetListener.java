package org.limewire.ui.swing.library.playlist;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;

import org.limewire.core.api.playlist.Playlist;
import org.limewire.ui.swing.dnd.LocalFileTransferable;

/**
 * A DropTargetListener installed on playlist buttons to accept drop operations.
 */
public class PlaylistDropTargetListener extends DropTargetAdapter {
    
    private final Playlist playlist;

    /**
     * Constructs a PlaylistDropTargetListener for the specified playlist.
     */
    public PlaylistDropTargetListener(Playlist playlist) {
        this.playlist = playlist;
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // Verify data flavors.
        if (//!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) &&
            !dtde.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR)) {
            dtde.rejectDrag();
            return;
        }

        // Accept drag if any files can be added.
        if (canAddFiles(dtde.getTransferable())) {
            dtde.acceptDrag(dtde.getDropAction());
        } else {
            dtde.rejectDrag();
        }
    }
    
    @Override
    public void drop(DropTargetDropEvent dtde) {
        // Verify data flavors.
        // TODO accept DataFlavor.javaFileListFlavor, add file to Library
        if (!dtde.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR)) {
            dtde.rejectDrop();
            return;
        }

        try {
            // Accept drop.
            dtde.acceptDrop(dtde.getDropAction());

            // Get files to be added.
            File[] files = getDropFiles(dtde.getTransferable());
            
            // Add files to playlist.
            for (File file : files) {
                if (playlist.canAdd(file)) {
                    playlist.addFile(file);
                }
            }

            // Drop completed with success.
            dtde.dropComplete(true);
            
        } catch (UnsupportedFlavorException ufx) {
            dtde.dropComplete(false);
        } catch (IOException iox) {
            dtde.dropComplete(false);
        }
    }
    
    /**
     * Returns true if the specified data contains any files that can be added
     * to the playlist.
     */
    private boolean canAddFiles(Transferable data) {
        try {
            // Get transfer files.
            File[] files = getDropFiles(data);
            
            // Return true if any file can be added.
            for (File file : files) {
                if (playlist.canAdd(file)) {
                    return true;
                }
            }

            // Return false if no files can be added.
            return false;
        
        } catch (UnsupportedFlavorException ufx) {
            return false;
        } catch (IOException iox) {
            return false;
        }
    }
    
    /**
     * Returns the array of files associated with the specified transfer data.
     */
    private File[] getDropFiles(Transferable data) 
        throws UnsupportedFlavorException, IOException {
        // Get transfer files based on data flavor.
        if (data.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR)) {
            return (File[]) data.getTransferData(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR);
            
        //} else if (data.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        //    List fileList = (List) data.getTransferData(DataFlavor.javaFileListFlavor);
        //    return (File[]) fileList.toArray();
        }

        // Return empty array if no files to transfer.
        return new File[0];
    }
}
