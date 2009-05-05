package org.limewire.xmpp.client.impl.messages.authtoken;

import org.limewire.xmpp.client.impl.XMPPConnectionImpl;

public interface AuthTokenIQListenerFactory {
    AuthTokenIQListener create(XMPPConnectionImpl connection);
}
