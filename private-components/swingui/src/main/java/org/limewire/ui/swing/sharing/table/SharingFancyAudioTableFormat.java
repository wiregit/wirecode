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
        if(column == 0) {
            String name = (String) baseObject.getProperty(FileItem.Keys.AUTHOR);
            if(name != null)
                return name;
            else 
                return baseObject.getName();
        }else if(column == 1) return baseObject.getProperty(FileItem.Keys.TITLE);
        else if(column == 2) return baseObject.getProperty(FileItem.Keys.ALBUM);
        else if(column == 3) return baseObject;
        
        throw new IllegalStateException("Unknown column:" + column);
    }

}
