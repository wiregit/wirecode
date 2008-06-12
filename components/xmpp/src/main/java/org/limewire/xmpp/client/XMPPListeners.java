package org.limewire.xmpp.client;

public interface XMPPListeners {
    RosterListener getRosterListener();
    PresenceListener getPresenceListener();
    LibraryListener getLibraryListener();
}
