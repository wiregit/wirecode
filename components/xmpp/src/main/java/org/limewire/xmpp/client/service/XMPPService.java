package org.limewire.xmpp.client.service;

import java.util.List;

import com.google.inject.Inject;

public interface XMPPService {
    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration);
    public List<XMPPConnection> getConnections();
    //public void register(RosterListener rosterListener);
    @Inject
    public void register(XMPPErrorListener errorListener);
    @Inject
    public void register(FileOfferHandler offerHandler);
}
