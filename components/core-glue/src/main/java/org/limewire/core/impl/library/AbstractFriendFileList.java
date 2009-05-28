package org.limewire.core.impl.library;

import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.SharedFileCollection;

abstract class AbstractFriendFileList extends LocalFileListImpl {

    AbstractFriendFileList(EventList<LocalFileItem> eventList, CoreLocalFileItemFactory fileItemFactory) {
        super(eventList, fileItemFactory);
    }

    // upgrade to require FriendFileList
    @Override
    abstract protected SharedFileCollection getMutableCollection();
    
}
