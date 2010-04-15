package org.limewire.security.id;

import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;

public interface SecureIdManager {

    public static final String SIG_KEY_ALGO = "RSA";

    public static final String SIG_ALGO = "SHA1withRSA";

    public static final String HASH_ALGO = "MD5";

    public static final String MAC_ALGO = "HmacMD5";

    public static final String ENCRYPTION_KEY_ALGO = "AES";

    // to make it more secure, we could use "AES/CBC/PKCS5Padding", 
    // but we will need to deal with transferring IV. 
    public static final String ENCRYPTION_ALGO = "AES";

    public static final int SIGNATURE_KEY_SIZE = 768;

    public static final boolean TAGGING = false;

     /**
     * @return if the local node knows the remoteID and shares a key with the remote node
     */
    public boolean isKnown(GUID remoteID);

    /**
     * @return hmac value
     * @throws Exception when remoteID not known 
     */
    public byte[] createHmac(GUID remoteID, byte[] data);

    /**
     * @return true if the data can be authenticated, i.e., the remoteID generated the hmac using the data.  
     * @throws Exception when remoteID not known 
     */
    public boolean verifyHmac(GUID remoteId, byte[] data, byte[] hmacValue);

    /**
     * @return ciphertext 
     * @throws Exception when remoteID not known 
     * @throws InvalidData 
     */
    public byte[] encrypt(GUID remoteId, byte[] plaintext)
            throws InvalidDataException;

    /**
     * @return plaintext 
     * @throws Exception when remoteID not known 
     * @throws InvalidData 
     */
    public byte[] decrypt(GUID remoteId, byte[] ciphertext)
            throws InvalidDataException;

    /**
     * @return the signature 
     */
    public byte[] sign(byte[] data);

    /**
     * @return true if the data can be authenticated, i.e., the remoteID generated the signature using the data.
     * @throws Exception when remoteID not known 
     * @throws InvalidData 
     */
    public boolean verifySignature(GUID remoteId, byte[] data, byte[] signature) throws InvalidDataException;

    public Identity getLocalIdentity();

    public GUID getLocalGuid();

    /**
     * process a remote node's identity:
     * 1) verify the remote node's id against its signature public key
     * 2) verify the signature
     * 3) store the identity if it is not in my list
     * @param identity
     * @return true if the remote node's identity is valid based on step 1) and 2). 
     */
    public boolean addIdentity(Identity identity);
}