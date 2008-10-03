package org.limewire.ui.swing.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

public interface MyLibraryFactory {
    
    MyLibraryPanel createMyLibrary(Category category, EventList<LocalFileItem> eventList);

}
