package com.limegroup.gnutella.dht.db;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.io.Address;
import org.limewire.io.GUID;

public interface AddressFinder {

    ListeningFuture<Address> search(GUID guid);
}
