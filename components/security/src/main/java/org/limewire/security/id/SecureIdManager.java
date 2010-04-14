package org.limewire.security.id;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidParameterSpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.limewire.io.GUID;

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
     * initializing DH community parameter, keys, and remote key storage.
     * @throws NoSuchAlgorithmException
     * @throws InvalidParameterSpecException
     * @throws IOException
     */
    public void start() throws NoSuchAlgorithmException, InvalidParameterSpecException,
            IOException;

    public void stop();

    /**
     * @return if the local node knows the remoteID and shares a key with the remote node
     */
    public boolean exist(GUID remoteID);

    /**
     * @return hmac value
     * @throws NoSuchAlgorithmException 
     */
    public byte[] createHmac(GUID remoteID, byte[] data) throws NoSuchAlgorithmException;

    /**
     * @return true if the data can be authenticated, i.e., the remoteID generated the hmac using the data.  
     * @throws NoSuchAlgorithmException 
     */
    public boolean verifyHmac(GUID remoteId, byte[] data, byte[] hmacValue)
            throws NoSuchAlgorithmException;

    /**
     * @return ciphertext 
     * @throws NoSuchPaddingException 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     */
    public byte[] encrypt(GUID remoteId, byte[] plaintext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException;

    /**
     * @return plaintext 
     * @throws NoSuchPaddingException 
     * @throws NoSuchAlgorithmException 
     * @throws InvalidKeyException 
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     */
    public byte[] decrypt(GUID remoteId, byte[] ciphertext)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException;

    /**
     * @return true if the data can be authenticated, i.e., the remoteID generated the signature using the data.  
     * @throws NoSuchAlgorithmException 
     */
    public boolean verifySignature(GUID remoteId, byte[] data, byte[] signature)
            throws NoSuchAlgorithmException;

    public Identity getLocalIdentity();

    /**
     * process a remote node's identity:
     * 1) verify the remote node's id against its signature public key
     * 2) verify the signature
     * 3) store the identity if it is not in my list
     * @param identity
     * @return true if the remote node's identity is valid based on step 1) and 2). 
     * @throws NoSuchAlgorithmException 
     */
    public boolean processIdentity(Identity identity) throws NoSuchAlgorithmException;

    /**
     * @return true if the signature is valid  
     * @throws NoSuchAlgorithmException 
     */
    public boolean verifySignature(byte[] data, byte[] signature, PublicKey publicKey)
            throws NoSuchAlgorithmException;

    /**
     * @return the signature 
     * @throws NoSuchAlgorithmException 
     */
    public byte[] sign(byte[] data) throws NoSuchAlgorithmException;

    public GUID getLocalId();

}