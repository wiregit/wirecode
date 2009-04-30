package org.limewire.core.api.friend.feature.features;

import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.xmpp.api.client.XMPPException;

public interface FileOfferer {
    void offerFile(FileMetaData fileMetaData) throws XMPPException;
}
