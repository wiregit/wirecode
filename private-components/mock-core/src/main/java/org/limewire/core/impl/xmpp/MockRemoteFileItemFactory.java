package org.limewire.core.impl.xmpp;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.InvalidDataException;

public class MockRemoteFileItemFactory implements RemoteFileItemFactory {

    @Override
    public RemoteFileItem create(FriendPresence presence, FileMetaData fileMetaData)
            throws InvalidDataException, SaveLocationException {
        // TODO Auto-generated method stub
        return null;
    }

}
