package org.limewire.xmpp.client.service;

/**
 * Allows the user of the xmpp service to approve / deny file offers
 */
public interface IncomingFileAcceptor {
    public boolean accept(FileMetaData f);
}
