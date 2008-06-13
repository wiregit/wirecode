package org.limewire.xmpp.client;

public interface LimePresence extends Presence {

    void sendFile(java.io.File file);

    void addLibraryListener(LibraryListener libraryListener);
}
