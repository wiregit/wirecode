package org.limewire.ui.swing.sharing.friends;

import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.util.I18n;

/**
 * Format for displaying Buddy Name and number of files shared with
 * that friend.
 */
public class FriendTableFormat extends AbstractTableFormat<FriendItem> {

    public FriendTableFormat() {
        super(I18n.tr("Name"), I18n.tr("Size"));
    }

    @Override
    public Object getColumnValue(FriendItem baseObject, int column) {
        if(column == 0) return baseObject;
        else if(column == 1) return baseObject;
            
        throw new IllegalStateException("Unknown column:" + column);
    }   
}