package org.limewire.xmpp.api.client;

import java.util.List;

public interface XMPPService {
    public void addConnectionConfiguration(XMPPConnectionConfiguration configuration);
    public List<XMPPConnection> getConnections();
    //public void register(RosterListener rosterListener);
    public void setXmppErrorListener(XMPPErrorListener errorListener);
    public void setFileOfferHandler(FileOfferHandler offerHandler);
    public void setConnectionListener(XMPPConnectionListener connectionListener);
    public XMPPConnectionListener getConnectionListener();
}
