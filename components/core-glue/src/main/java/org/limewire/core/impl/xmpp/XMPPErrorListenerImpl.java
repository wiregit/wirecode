package org.limewire.core.impl.xmpp;

import com.google.inject.Singleton;
import org.limewire.xmpp.client.impl.XMPPException;
import org.limewire.xmpp.client.service.XMPPErrorListener;

@Singleton
class XMPPErrorListenerImpl implements XMPPErrorListener{
    public void error(XMPPException exception) {
        exception.printStackTrace();
    }
}
