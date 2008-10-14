package org.limewire.ui.swing.sharing.fancy;

import javax.swing.JScrollPane;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;

import ca.odell.glazedlists.EventList;

public interface SharingFancyPanelFactory {
    
    SharingFancyPanel create(EventList<LocalFileItem> eventList, 
            JScrollPane scrollPane, LocalFileList originalList);
    
}
