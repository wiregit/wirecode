package org.limewire.xmpp.client.impl;

import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;

/**
 * Creates XMPPConnectionImpls.  Used i conjunction with @AssistedInject
 */
public interface XMPPConnectionImplFactory {
    XMPPConnectionImpl createConnection(FriendConnectionConfiguration configuration,
                                        ListeningExecutorService executorService);
        
}
