package org.limewire.xmpp.client.impl.messages.address;

import org.limewire.xmpp.client.impl.XMPPConnectionImpl;
import org.limewire.net.address.AddressFactory;

public interface AddressIQListenerFactory {
    AddressIQListener create(XMPPConnectionImpl connection, AddressFactory factory);
}
