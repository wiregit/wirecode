/**
 * 
 */
package org.limewire.security.id;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;

class RemoteIdKeys {
    private final PublicKey signaturePublicKey;

    private final SecretKey hmacKey;

    private final SecretKey encryptionKey;

    private final GUID id;
    
    public RemoteIdKeys(byte[] data) throws InvalidDataException {
        try {
            GGEP ggep = new GGEP(data);
            id = new GUID(ggep.getBytes("ID"));
            KeyFactory factory = KeyFactory.getInstance(SecureIdManager.SIG_KEY_ALGO);
            signaturePublicKey = factory.generatePublic(new X509EncodedKeySpec(ggep.getBytes("SPK")));
            hmacKey = new SecretKeySpec(ggep.getBytes("HMAC"), SecureIdManager.MAC_ALGO);
            encryptionKey = new SecretKeySpec(ggep.getBytes("ENC"), SecureIdManager.ENCRYPTION_KEY_ALGO);
        } catch (BadGGEPBlockException e) {
            throw new InvalidDataException(e);
        } catch (BadGGEPPropertyException e) {
            throw new InvalidDataException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidDataException(e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidDataException(e);
        }
    }
    
    public RemoteIdKeys(GUID id, PublicKey pk, SecretKey hmacKey, SecretKey encryptionKey) {
        this.id = id;
        signaturePublicKey = pk;
        this.hmacKey = hmacKey;
        this.encryptionKey = encryptionKey;
    }

    public GUID getId() {
        return id;
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

    public byte[] toByteArray() {
        GGEP ggep = new GGEP();
        ggep.put("ID", id.bytes());
        ggep.put("SPK", signaturePublicKey.getEncoded());
        ggep.put("HMAC", hmacKey.getEncoded());
        ggep.put("ENC", encryptionKey.getEncoded());
        return ggep.toByteArray();
    }
}