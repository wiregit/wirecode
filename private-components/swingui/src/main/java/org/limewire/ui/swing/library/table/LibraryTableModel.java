package org.limewire.ui.swing.library.table;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.AbstractTableFormat;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

public class LibraryTableModel<T extends FileItem>  extends EventTableModel<T> {

    private final EventList<? extends FileItem> libraryItems;
    public static final int NAME_COL = 0;
    public static final int SIZE_COL = NAME_COL + 1;
    public static final int CREATED_COL = SIZE_COL + 1;
    public static final int MODIFIED_COL = CREATED_COL + 1;
    public static final int SHARE_COL = MODIFIED_COL + 1;
    
    public LibraryTableModel(EventList<T> libraryItems) {
        super(libraryItems, new LibraryTableFormat<T>());
        this.libraryItems = libraryItems;
    }
    
    public FileItem getFileItem(int index) {
        return libraryItems.get(index);
    }
    
    private static class LibraryTableFormat<T extends FileItem>  extends AbstractTableFormat<T> {

        public LibraryTableFormat() {
            super(tr("Name"), tr("Size"), tr("Created"), tr("Modified"), "");
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
