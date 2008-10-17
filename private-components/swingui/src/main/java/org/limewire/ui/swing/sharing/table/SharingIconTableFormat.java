package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for a generic fileName. The fileName is displayed along with
 * its icon. The second column is a list of actions that can operate on
 * this fileItem.
 */
public class SharingIconTableFormat extends AbstractTableFormat<LocalFileItem> {
  
    private static final int FILENAME_INDEX = 0;
    private static final int ACTIONS_INDEX = 1;
    
    public SharingIconTableFormat() {
        super(I18n.tr("Name"), "");
    }
    
    @Override
    public Object getColumnValue(LocalFileItem fileItem, int column) {
        switch(column) {
            case FILENAME_INDEX: return fileItem;
            case ACTIONS_INDEX: return fileItem;
        }
        
        throw new IllegalStateException("Unknown column:" + column);
    }
}
