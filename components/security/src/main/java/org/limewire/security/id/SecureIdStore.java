package org.limewire.security.id;

import org.limewire.io.GUID;


public interface SecureIdStore {

    void setLocalData(byte[] value);
    
    byte[] getLocalData();
    
    public void put(GUID key, byte[] value);
    
    byte[] get(GUID key);
}
