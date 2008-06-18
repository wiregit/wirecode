package org.limewire.xmpp.client;

public interface LimePresence extends Presence {
    
    void requestFile(File file, FileTransferProgressListener progressListener);

    void sendFile(java.io.File file, FileTransferProgressListener progressListener);

    void setLibraryListener(LibraryListener libraryListener);
}
