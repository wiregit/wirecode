package org.limewire.ui.swing.sharing.friends;

import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

public interface BuddyUpdate {

    public void setBuddyName(String name);
    
    public void setEventList(EventList<LocalFileItem> model);
}
