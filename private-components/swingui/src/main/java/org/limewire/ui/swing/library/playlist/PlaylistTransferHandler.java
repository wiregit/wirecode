package org.limewire.ui.swing.library.playlist;

import java.awt.Component;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.ui.swing.dnd.LocalFileTransferable;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * An implementation of TransferHandler for playlists.  This handles user
 * actions to reorder the files in a playlist. 
 */
public class PlaylistTransferHandler extends TransferHandler {
    
    private final Playlist playlist;

    /**
     * Constructs a PlaylistTransferHandler.
     */
    public PlaylistTransferHandler(Playlist playlist) {
        this.playlist = playlist;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        if (c instanceof PlaylistLibraryTable) {
            PlaylistLibraryTable<?> libTable = (PlaylistLibraryTable<?>) c;
            
            // Get list of selected items.
            EventSelectionModel<?> selectionModel = (EventSelectionModel<?>) libTable.getSelectionModel();
            EventList<?> selectedList = selectionModel.getSelected();
            
            // Get files for transfer.
            File[] files = new File[selectedList.size()];
            for (int i = 0; i < selectedList.size(); i++) {
                LocalFileItem fileItem = (LocalFileItem) selectedList.get(i);
                files[i] = fileItem.getFile();
            }
            
            // Create transferable data.
            return new LocalFileTransferable(files);
        }
        
        return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public void exportAsDrag(JComponent comp, InputEvent e, int action) {
        super.exportAsDrag(comp, e, action);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // TODO if move then remove data?
        //System.out.println("exportDone: source=" + source.getClass().getSimpleName() + ", action=" + action);
        super.exportDone(source, data, action);
    }

    @Override
    public boolean canImport(TransferSupport support) {
        boolean canImport = support.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR);
        
        if (canImport && support.isDrop()) {
            support.setShowDropLocation(false);
        }
        
        return canImport;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }
        
        Component component = support.getComponent();
        if (!(component instanceof PlaylistLibraryTable)) {
            return false;
        }
        
        if (support.isDataFlavorSupported(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR)) {
            PlaylistLibraryTable<?> libTable = (PlaylistLibraryTable<?>) component;
            int dropRow = libTable.rowAtPoint(support.getDropLocation().getDropPoint());
            
            try {
                Transferable data = support.getTransferable();
                File[] files = (File[]) data.getTransferData(LocalFileTransferable.LOCAL_FILE_DATA_FLAVOR);

                // TODO REMOVE DEBUGGING
                //System.out.println("importData: dropRow=" + dropRow + ", items=" + files.length);
                                
                for (File file : files) {
                    //System.out.println("-> adding " + file.getAbsolutePath());
                    // TODO eventList.add(dropRow, fileItem);
                    playlist.addFile(dropRow, file);
                }
                
                return true;
                
            } catch (Exception ex) {
                // TODO log error
                ex.printStackTrace();
                return false;
            }
        }

        // Other data flavors not supported so return failure.
        return false;
    }
}
