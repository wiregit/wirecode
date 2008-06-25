package org.limewire.xmpp.client;

public interface LimePresence extends Presence {
    
    void requestFile(FileMetaData file, FileTransferProgressListener progressListener);

    void sendFile(FileMetaData file, FileTransferProgressListener progressListener);

    void setLibraryListener(LibraryListener libraryListener);
}
