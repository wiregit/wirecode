package org.limewire.security.id;

import java.util.HashMap;

import org.limewire.io.GUID;
/**
 * a simple SecureIdStore impl used in tests.  
 */
public class SimpleSecureIdStoreImpl implements SecureIdStore {
    private HashMap<GUID, byte[]> remoteKeys;
    private byte[] privateIdentityBytes;
    
    public SimpleSecureIdStoreImpl(){
        remoteKeys = new HashMap<GUID, byte[]>();
    }
    
    public byte[] get(GUID key) {
        byte[] temp = remoteKeys.get(key); 
        return temp;
    }
    
    public void put(GUID key, byte[] value) {
        remoteKeys.put(key, value);
    }
    
    public byte[] getLocalData() {
        return privateIdentityBytes;
    }   
    
    public void setLocalData(byte[] value) {
        privateIdentityBytes = value;
    }
}
