package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Decides what data to display in the Audio Format
 */
public class SharingFancyAudioTableFormat implements TableFormat<FileItem> {

    public static final String[] columnLabels = new String[] {"Artist", "Song", "Album",""};
    
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
    public Object getColumnValue(FileItem baseObject, int column) {
        if(column == 0) return baseObject.getName();
        else if(column == 1) return baseObject.getSize();
        else if(column == 2) return baseObject.getCreationTime();
        else if(column == 3) return baseObject;
        
        throw new IllegalStateException("Unknown column:" + column);
    }

}
