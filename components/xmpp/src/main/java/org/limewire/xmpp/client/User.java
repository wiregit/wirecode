package org.limewire.xmpp.client;

public interface User {
    String getId();

    String getName();

    void addPresenceListener(PresenceListener presenceListener);

}
