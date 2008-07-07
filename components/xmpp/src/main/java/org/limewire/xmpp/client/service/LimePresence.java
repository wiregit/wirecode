package org.limewire.xmpp.client.service;

/**
 * Marks a presence as running in limewire.  Allows for additional limewire
 * specific features.
 */
public interface LimePresence extends Presence {

    /**
     * Request a file from this user
     * @param file
     * @param progressListener
     */
    public void requestFile(FileMetaData file, FileTransferProgressListener progressListener);

    /**
     * send a file to this user
     * @param file
     * @param progressListener
     */
    public void sendFile(FileMetaData file, FileTransferProgressListener progressListener);

    /**
     * Register a <code>LibraryListener</code> on this user, so as to be
     * notified about the files that user is sharing
     * @param libraryListener
     */
    public void setLibraryListener(LibraryListener libraryListener);
}
