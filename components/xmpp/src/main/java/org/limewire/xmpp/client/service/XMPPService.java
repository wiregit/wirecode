package org.limewire.xmpp.client.service;

import java.util.List;

public interface XMPPService {
    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration);
    public List<XMPPConnection> getConnections();
}
