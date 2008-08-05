package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class SharingTableModel extends EventTableModel<FileItem> {

    private final EventList<FileItem> sharedItems;
    private static final int COLUMN_COUNT = 4;
    
    public SharingTableModel(EventList<FileItem> sharedItems) {
        super(sharedItems, new SharingTableFormat());
        this.sharedItems = sharedItems;
    }
    
    public FileItem getFileItem(int index) {
        return sharedItems.get(index);
    }
    
    private static class SharingTableFormat implements TableFormat<FileItem> {

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            if(column == 0) return "Name";
            else if(column == 1) return "Size";
            else if(column == 2) return "Created";
            else if(column == 3) return "Modified";
            
            throw new IllegalStateException("Unknown column:" + column);
        }

        @Override
        public Object getColumnValue(FileItem baseObject, int column) {
            if(column == 0) return baseObject.getName();
            else if(column == 1) return baseObject.getSize();
            else if(column == 2) return baseObject.getCreationTime();
            else if(column == 3) return baseObject.getLastModifiedTime();
            
            throw new IllegalStateException("Unknown column:" + column);
        }        
    }

}
