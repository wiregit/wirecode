package org.limewire.xmpp.client;

/**
 * Allows the user of the xmpp service to approve / deny file offers
 */
public interface IncomingFileAcceptor {
    public boolean accept(FileMetaData f);
}
