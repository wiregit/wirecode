package org.limewire.security.id;

import org.limewire.io.GUID;

public interface SecureIdStore {

    public LocalIdentity getLocalIdentity();
    
    public void setLocalIdentity(LocalIdentity identity);
    
    public RemoteIdKeys getRemoteIdKeys(GUID id);
    
    public void storeRemoteIdKeys(RemoteIdKeys remoteKeys);
    
}
