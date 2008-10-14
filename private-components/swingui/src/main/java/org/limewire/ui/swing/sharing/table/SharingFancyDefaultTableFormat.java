package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * Default information to display in a table for what is being shared. 
 * Only the file name is displayed by default. The second column
 * can display a list of actions that can operate on this fileItem.
 */
public class SharingFancyDefaultTableFormat extends AbstractTableFormat<LocalFileItem> {
  
    private static final int FILENAME_INDEX = 0;
    private static final int ACTIONS_INDEX = 1;
    
    public SharingFancyDefaultTableFormat() {
        super(I18n.tr("Name"), "");
    }
    
    @Override
    public Object getColumnValue(LocalFileItem fileItem, int column) {
        switch(column) {
            case FILENAME_INDEX: return fileItem.getName();
            case ACTIONS_INDEX: return fileItem;
        }
        
        throw new IllegalStateException("Unknown column:" + column);
    }
}
