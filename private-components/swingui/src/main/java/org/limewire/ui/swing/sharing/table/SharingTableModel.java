package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class SharingTableModel extends EventTableModel<FileItem> {

    private final EventList<FileItem> sharedItems;
    
    public static final String[] columnLabels = new String[] {"Name", "Size", "Created", "Modified", "Hits", "Uploads", "Actions"};
    
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
            else if(column == 3) return baseObject.getLastModifiedTime();
            else if(column == 4) return baseObject.getNumHits();
            else if(column == 5) return baseObject.getNumUploads();
            else if(column == 6) return baseObject;
            
            throw new IllegalStateException("Unknown column:" + column);
        }        
    }

}
