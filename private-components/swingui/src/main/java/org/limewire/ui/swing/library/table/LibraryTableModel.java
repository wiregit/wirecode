package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class LibraryTableModel<T extends FileItem>  extends EventTableModel<T> {


    private final EventList<? extends FileItem> libraryItems;
    public static final int NAME_COL = 0;
    public static final int SIZE_COL = NAME_COL + 1;
    public static final int CREATED_COL = SIZE_COL + 1;
    public static final int MODIFIED_COL = CREATED_COL + 1;
    public static final int SHARE_COL = MODIFIED_COL + 1;
    private static final int COLUMN_COUNT = SHARE_COL + 1;
    
    public LibraryTableModel(EventList<T> libraryItems) {
        super(libraryItems, new LibraryTableFormat<T>());
        this.libraryItems = libraryItems;
    }
    
    public FileItem getFileItem(int index) {
        return libraryItems.get(index);
    }
    
    private static class LibraryTableFormat<T extends FileItem> implements TableFormat<T> {

        @Override
        public int getColumnCount() {
            return COLUMN_COUNT;
        }

        @Override
        public String getColumnName(int column) {
            if(column == NAME_COL) return "Name";
            else if(column == SIZE_COL) return "Size";
            else if(column == CREATED_COL) return "Created";
            else if(column == MODIFIED_COL) return "Modified";
            else if(column == SHARE_COL) return "";
            
            throw new IllegalStateException("Unknown column:" + column);
        }

        @Override
        public Object getColumnValue(FileItem baseObject, int column) {
            if(column == NAME_COL) return baseObject.getName();
            else if(column == SIZE_COL) return baseObject.getSize();
            else if(column == CREATED_COL) return baseObject.getCreationTime();
            else if(column == MODIFIED_COL) return baseObject.getLastModifiedTime();
            else if(column == SHARE_COL) return baseObject;
            
            throw new IllegalStateException("Unknown column:" + column);
        }        
    }

}
