package org.limewire.core.api.xmpp;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.core.api.search.SearchResult;
import org.limewire.io.InvalidDataException;

public interface FileMetaDataConverter {
    
    /** Converts FileMetaData into a SearchResult. */
    SearchResult create(FriendPresence presence, FileMetaData fileMetaData) throws InvalidDataException, SaveLocationException;
}
