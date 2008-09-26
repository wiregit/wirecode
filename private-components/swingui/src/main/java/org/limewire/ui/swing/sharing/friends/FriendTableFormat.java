package org.limewire.ui.swing.sharing.friends;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Format for displaying Buddy Name and number of files shared with
 * that friend.
 */
public class FriendTableFormat implements TableFormat<FriendItem> {

    public static final String[] columnLabels = new String[] {"Name", "Size"};
            
    @Override
    public int getColumnCount() {
        return columnLabels.length;
    }

    @Override
    public String getColumnName(int column) {
        if(column < 0 || column >= columnLabels.length)
            throw new IllegalStateException("Unknown column:" + column);

        return columnLabels[column];
    }

    @Override
    public Object getColumnValue(FriendItem baseObject, int column) {
        if(column == 0) return baseObject;
        else if(column == 1) return baseObject;
            
        throw new IllegalStateException("Unknown column:" + column);
    }   
}