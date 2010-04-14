package org.limewire.security.id;

import org.limewire.io.GUID;

public interface SecureIdStore {

    LocalIdentity getLocalIdentity();
    
    void setLocalIdenity(LocalIdentity identity);
    
    Identity getIdentity(GUID id);
    
    void storeIdentity(Identity identity);
}
