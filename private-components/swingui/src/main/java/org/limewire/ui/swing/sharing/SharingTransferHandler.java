package org.limewire.ui.swing.sharing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.TransferHandler;

import org.limewire.core.api.library.LibraryManager;

/**
 * A transfer handler for adding shared files to a table.
 */
public class SharingTransferHandler extends TransferHandler {
    
    private LibraryManager libraryManager;
    
    public SharingTransferHandler(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }

    /**
     * Currently only support a list of Files to be imported to this component
     */
    @Override
    public boolean canImport(TransferHandler.TransferSupport info) {
        if(!info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return false;
        }
        return true;
    }
    
    /**
     * Performs the actual importing of the files. Note the files are not
     * directly added to the Model but rather added to the FileMananager first.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferHandler.TransferSupport info) {
        if(!info.isDrop())
            return false;
        
        Transferable transferable = info.getTransferable();
        List<File> list;
        try {
            list = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (UnsupportedFlavorException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        
        for(File f : list) {
            libraryManager.addGnutellaFile(f);
        }
        
        return true;
    }
}
