package org.limewire.ui.swing.library;

import javax.swing.JComponent;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MyLibraryMediator extends BaseLibraryMediator {

    private MyLibraryFactory factory;
    
    @Inject
    public MyLibraryMediator(MyLibraryFactory factory) {
        this.factory = factory;
    }
    
    public void setMainCardEventList(Friend friend, EventList<LocalFileItem> eventList) {
        setLibraryCard(factory.createMyLibrary(friend, eventList));
    }
    
    @Override
    public void setSharingCard(JComponent panel) {
        // do nothing, no card can be added here for MyLibrary
    }
}
