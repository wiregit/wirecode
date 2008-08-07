package org.limewire.xmpp.api.client;

import java.util.List;

public interface XMPPService {
    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration);
    public List<XMPPConnection> getConnections();
    //public void register(RosterListener rosterListener);
    public void register(XMPPErrorListener errorListener);
    public void register(FileOfferHandler offerHandler);
}
