package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteFileItem;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryMediator extends BaseLibraryMediator {

    private FriendLibraryFactory factory;
    private Friend friend;
    
    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory) {
        this.factory = factory;
        this.friend = friend;
        
        setMainCard(new FriendEmptyLibrary(friend));
    }
    
    public void createLibraryPanel(EventList<RemoteFileItem> eventList) {
        JComponent component = factory.createFriendLibrary(friend, eventList);
        setAuxCard(component);
        showAuxCard();
    }
}
