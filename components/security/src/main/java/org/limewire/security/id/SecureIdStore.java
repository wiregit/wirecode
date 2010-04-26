package org.limewire.security.id;

import org.limewire.io.GUID;

/**
 * This is the storage of a SecureIdManager. It stores both the load identity
 * and the remoteIdKeys the local node shares with other nodes in the network.
 */
public interface SecureIdStore {

    void setLocalData(byte[] value);
    
    byte[] getLocalData();
    
    public void put(GUID key, byte[] value);
    
    byte[] get(GUID key);
}
