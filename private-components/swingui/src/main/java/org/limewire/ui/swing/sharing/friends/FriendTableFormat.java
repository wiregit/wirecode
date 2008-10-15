package org.limewire.ui.swing.sharing.friends;

import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * Format for displaying Buddy Name and number of files shared with
 * that friend.
 */
public class FriendTableFormat extends AbstractTableFormat<FriendItem> {

    private static final int NAME_INDEX = 0;
    private static final int SIZE_INDEX = 1;
    
    public FriendTableFormat() {
        super(I18n.tr("Name"), I18n.tr("Size"));
    }

    @Override
    public Object getColumnValue(FriendItem baseObject, int column) {
        switch(column) {
            case NAME_INDEX:
            case SIZE_INDEX:
                return baseObject;
        }
            
        throw new IllegalStateException("Unknown column:" + column);
    }   
}