package org.limewire.ui.swing.sharing.friends;

import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

public interface FriendUpdate {

    public void setFriendName(String name);
    
    public void setEventList(EventList<LocalFileItem> model);
}
