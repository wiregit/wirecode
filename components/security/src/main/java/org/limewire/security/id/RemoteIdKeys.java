/**
 * 
 */
package org.limewire.security.id;

import java.security.PublicKey;

import javax.crypto.SecretKey;

import org.limewire.io.GUID;

class RemoteIdKeys{
    private final PublicKey signaturePublicKey;
    private final SecretKey hmacKey;
    private final SecretKey encryptionKey;
    private final GUID id;
    
    public RemoteIdKeys(GUID id, PublicKey pk, SecretKey hmacKey, SecretKey encryptionKey) {        
        this.id = id;
        signaturePublicKey = pk;
        this.hmacKey = hmacKey;
        this.encryptionKey = encryptionKey;
    }

    public PublicKey getSignaturePublicKey() {
        return signaturePublicKey;
    }

    public SecretKey getMacKey() {
        return hmacKey;
    }

    public SecretKey getEncryptionKey() {
        return encryptionKey;
    }
}