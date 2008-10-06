package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.gui.WritableTableFormat;

public interface LibraryTableFormat<T extends FileItem> extends WritableTableFormat<T>{
    
    int getActionColumn();
    
    int[] getDefaultHiddenColums();

}
