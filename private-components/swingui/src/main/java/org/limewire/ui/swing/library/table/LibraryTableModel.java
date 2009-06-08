package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class LibraryTableModel extends EventTableModel<LocalFileItem> {

    private final EventList<LocalFileItem> libraryItems;

    public LibraryTableModel(EventList<LocalFileItem> libraryItems, TableFormat<LocalFileItem> tableFormat) {
        super(libraryItems, tableFormat, false);
        this.libraryItems = libraryItems;
    }
    
    public LocalFileItem getFileItem(int index) {
        return libraryItems.get(index);
    }
    
    public EventList<LocalFileItem> getAllItems() {
        return libraryItems;
    }
}
