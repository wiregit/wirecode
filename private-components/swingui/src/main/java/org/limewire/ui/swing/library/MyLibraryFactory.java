package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

public interface MyLibraryFactory {
    
    JComponent createMyLibrary(Category category, EventList<LocalFileItem> eventList);

}
