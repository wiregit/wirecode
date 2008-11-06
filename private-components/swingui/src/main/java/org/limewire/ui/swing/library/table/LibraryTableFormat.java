package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.gui.WritableTableFormat;

public interface LibraryTableFormat<T extends FileItem> extends WritableTableFormat<T>{
    
    int getActionColumn();
    /**
     * Due to the inner workings of JXTable, these must be in reverse order for the hiding to work.  
     */
    int[] getDefaultHiddenColums();

}
