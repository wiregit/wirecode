package org.limewire.xmpp.client;

import java.util.List;

public interface XMPPService {
    void addConnectionConfiguration(XMPPConnectionConfiguration configuration);
    List<XMPPConnection> getConnections();
}
