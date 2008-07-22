package org.limewire.xmpp.client;

import org.limewire.xmpp.client.service.IncomingFileAcceptor;
import org.limewire.xmpp.client.service.FileMetaData;

import com.google.inject.Provider;

public class IncomingFileAcceptorImpl implements IncomingFileAcceptor, Provider<IncomingFileAcceptor> {
    public boolean accept(FileMetaData f) {
        return true;
    }

    public IncomingFileAcceptor get() {
        return this;
    }
}
