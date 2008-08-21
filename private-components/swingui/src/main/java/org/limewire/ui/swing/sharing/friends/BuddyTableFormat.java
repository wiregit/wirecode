package org.limewire.ui.swing.sharing.friends;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Format for displaying Buddy Name and number of files shared with
 * that buddy.
 */
public class BuddyTableFormat implements TableFormat<BuddyItem> {

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
    public Object getColumnValue(BuddyItem baseObject, int column) {
        if(column == 0) return baseObject;
        else if(column == 1) return baseObject;
            
        throw new IllegalStateException("Unknown column:" + column);
    }   
}