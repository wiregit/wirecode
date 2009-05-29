package org.limewire.core.impl.library;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.SharedFileCollection;

public class SharedFileListImpl extends LocalFileListImpl implements SharedFileList {
   
    private final SharedFileCollection coreCollection;

    public SharedFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory,
            SharedFileCollection gnutellaFileCollection) {
        super(new BasicEventList<LocalFileItem>(), coreLocalFileItemFactory);
        this.coreCollection = gnutellaFileCollection;
        this.coreCollection.addListener(newEventListener());
    }
    
    @Override
    protected FileCollection getCoreCollection() {
        return coreCollection;
    }
    
    @Override
    public void addFriend(Friend friend) {
        coreCollection.addFriend(friend.getId());
    }

    @Override
    public EventList<Friend> getFriends() {
        return new BasicEventList<Friend>();
    }

    @Override
    public String getName() {
        return coreCollection.getName();
    }

    @Override
    public boolean isNameChangeAllowed() {
        return coreCollection.getId() != 0; // TODO: Do better.
    }

    @Override
    public void removeFriend(Friend friend) {
        coreCollection.removeFriend(friend.getId());
    }

    @Override
    public void setName(String name) {
        coreCollection.setName(name);
    }

}