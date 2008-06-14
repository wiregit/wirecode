package org.limewire.xmpp.client;

public interface LimePresence extends Presence {
    
    void requestFile(File file);

    void sendFile(java.io.File file);

    void setLibraryListener(LibraryListener libraryListener);
}
