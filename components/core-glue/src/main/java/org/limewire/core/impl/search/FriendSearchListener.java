package org.limewire.core.impl.search;

import java.util.Iterator;

import org.limewire.core.api.library.RemoteFileItem;

public interface FriendSearchListener {
    public void handleFriendResults(Iterator<RemoteFileItem> results);
}
