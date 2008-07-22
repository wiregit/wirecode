package org.limewire.xmpp.client.service;

import java.io.IOException;

import org.limewire.net.address.Address;
import org.xmlpull.v1.XmlPullParserException;

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
    public FileTransferMetaData requestFile(FileMetaData file, FileTransferProgressListener progressListener) throws IOException, XmlPullParserException;

    /**
     * send a file to this user
     * @param file
     * @param progressListener
     */
    public void sendFile(FileTransferMetaData file, FileTransferProgressListener progressListener);

    /**
     * Register a <code>LibraryListener</code> on this user, so as to be
     * notified about the files that user is sharing
     * @param libraryListener
     */
    public void setLibraryListener(LibraryListener libraryListener);
    
    public Address getAddress();
}
