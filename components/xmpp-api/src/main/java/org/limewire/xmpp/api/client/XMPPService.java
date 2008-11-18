package org.limewire.xmpp.api.client;

import java.util.List;

public interface XMPPService {
    public List<XMPPConnection> getConnections();
    public void setXmppErrorListener(XMPPErrorListener errorListener);
}
