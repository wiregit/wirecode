package org.limewire.security.id;

import java.security.PrivateKey;

/**
 * PrivateIdentity contains the private information that a node 
 * uses to prove its identity and do key agreement. These information, 
 * however, shall not be send to other nodes. 
 * 
 * PrivateIdentiy also includes multiInstallationMark that is used 
 * together with the node's public signature key to generate the node's 
 * GUID.
 */
public interface PrivateIdentity extends Identity {
    
    public PrivateKey getPrivateSignatureKey();
    
    public PrivateKey getPrivateDiffieHellmanKey();
    
    public int getMultiInstallationMark();

    public byte[] toByteArray();
}
