package org.limewire.security.id;

import java.security.PrivateKey;

public interface PrivateIdentity extends Identity {
    
    public PrivateKey getPrivateSignatureKey();
    
    public PrivateKey getPrivateDiffieHellmanKey();
    
    public int getMultiInstallationMark();

    public byte[] toByteArray();
}
