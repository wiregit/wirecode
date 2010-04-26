package org.limewire.security.id;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.limewire.io.GUID;
import org.limewire.io.InvalidDataException;
import org.limewire.security.SecurityUtils;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;

public class SecureIdManagerImpl implements SecureIdManager {    
    /** 768-bit system-wide Diffie-Hellman parameters
     *  limewire base32 encoded    
     */
    private static final String DH_PARAMETER ="GCA4SATBACPJOPGPJPLAVQ5M37DLZKJ2MJOL5JFGQWUVF52ZEAZSNXJXWVCC7NVGWGVVEEPZBRLQ3GDOP7QZZDD2TP76T2D73UFCM3EYLSTBVYCV2FZNU7CVVQXM32YWZIM3FFJHC22EEED66XIVNHUHXF4TVH335RIQEYC732YJHTFBSNUNT4DJLCZT3NQBE564S76Y7FOOA5W7NTTWWTNX2GZLDNAFH4QKZDQ7NHZQJTKD4X4VULJP66EYLBRXZUCJFZRTRVNM4LLLAX3HEGRYFQ5GYJR73MDJGCQVKBJKZFC26OKTN325OWFD63DTAIBAF7Y";
    
    private DHParameterSpec dhParamSpec;
    private final SecureIdStore secureIdStore;
    private volatile PrivateIdentity localIdentity;
    
    @Inject
    public SecureIdManagerImpl(SecureIdStore secureIdStore) {
        this.secureIdStore = secureIdStore;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#start()
     */
    public void start() throws Exception{
        // init DH community parameter
        initDHParamSpec();
        // init my privateIdentity, first from locally stored data. 
        // if fail, then generate a new identity.
        try{
            localIdentity = new PrivateIdentityImpl(secureIdStore.getLocalData());
        } catch (Exception e){
            localIdentity = createPrivateIdentity();            
            secureIdStore.setLocalData(localIdentity.toByteArray());
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#exist(org.limewire.io.GUID)
     */
    public boolean isKnown(GUID remoteID){
        return secureIdStore.get(remoteID) != null;
    }
    
    
    public SecureIdStore getSecureIdStore(){
        return secureIdStore;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#createHmac(org.limewire.io.GUID, byte[])
     */
    public byte[] createHmac(GUID remoteId, byte[] data){
        if(! isKnown(remoteId)){
            throw new IllegalArgumentException("unknown ID "+remoteId);
        }
        
        try {
            Mac mac = Mac.getInstance(MAC_ALGO);
            mac.init((new RemoteIdKeys(secureIdStore.get(remoteId))).getMacKey());
            return mac.doFinal(data);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidDataException e) {
            throw new RuntimeException(e);
        }        
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#verifyHmac(org.limewire.io.GUID, byte[], byte[])
     */
    public boolean verifyHmac(GUID remoteId, byte[] data, byte[] hmacValue) {
        if(! isKnown(remoteId)){
            throw new IllegalArgumentException("unknown ID "+remoteId);
        }
        try {
            Mac mac = Mac.getInstance(MAC_ALGO);
            mac.init((new RemoteIdKeys(secureIdStore.get(remoteId))).getMacKey());
            return Arrays.equals(mac.doFinal(data), hmacValue);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidDataException e) {
            throw new RuntimeException(e);
        }        
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#encrypt(org.limewire.io.GUID, byte[])
     */
    public byte[] encrypt(GUID remoteId, byte[] plaintext){
        if(! isKnown(remoteId)){
            throw new IllegalArgumentException("unknown ID "+remoteId);
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
            
            cipher.init(Cipher.ENCRYPT_MODE, (new RemoteIdKeys(secureIdStore.get(remoteId))).getEncryptionKey());
            byte[] encrypted = cipher.doFinal(plaintext);
            return encrypted;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidDataException e) {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#decrypt(org.limewire.io.GUID, byte[])
     */
    public byte[] decrypt(GUID remoteId, byte[] ciphertext) throws InvalidDataException{
        if(! isKnown(remoteId)){
            throw new IllegalArgumentException("unknown ID "+remoteId);
        }
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, (new RemoteIdKeys(secureIdStore.get(remoteId))).getEncryptionKey());
            byte[] plaintext = cipher.doFinal(ciphertext);
            return plaintext;
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new InvalidDataException("bad ciphertext");
        } catch (InvalidDataException e) {
            throw new RuntimeException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#verifySignature(org.limewire.io.GUID, byte[], byte[])
     */
    public boolean verifySignature(GUID remoteId, byte [] data, byte [] signature){
        if(! isKnown(remoteId)){
            throw new IllegalArgumentException("unknown ID "+remoteId);
        }
        try {
            Signature verifier = Signature.getInstance(SIG_ALGO);
            verifier.initVerify((new RemoteIdKeys(secureIdStore.get(remoteId))).getSignaturePublicKey());
            verifier.update(data);
            return verifier.verify(signature);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidDataException e) {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#verifySignature(byte[], byte[], java.security.PublicKey)
     */
    public boolean verifySignature(PublicKey publicKey, byte [] data, byte [] signature) {
        try {
            Signature verifier = Signature.getInstance(SIG_ALGO);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#getMyIdentity()
     */
    public Identity getPublicLocalIdentity(){
        return new IdentityImpl(localIdentity.getGuid(), localIdentity.getPublicSignatureKey(), localIdentity.getPublicDiffieHellmanComponent(), localIdentity.getSignature());
    }

    /** 
     * returns private local identity which includes private keys 
     */
    PrivateIdentity getPrivateLocalIdentity(){
        return localIdentity;
    }

    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#processIdentity(org.limewire.security.id.Identity)
     */
    public boolean addIdentity(Identity identity){
        RemoteIdKeys rpe = verifyIdentityAndDoKeyAgreement(identity);
        if(rpe == null){// bad identify
            return false;
        }else{
            if(!isKnown(identity.getGuid())){
                secureIdStore.put(identity.getGuid(), rpe.toByteArray());
            }
            return true;
        }
    }

    private RemoteIdKeys createRemoteIdKeys(GUID remoteId, PublicKey pk, byte[] sharedSecret){
        // generate all the symmetric keys
        try{
            MessageDigest md = MessageDigest.getInstance(HASH_ALGO);        
            md.update(StringUtils.toUTF8Bytes("AUTH"));
            md.update(sharedSecret);        
            byte[] macSecret = md.digest();
            
            md.reset();
            md.update(StringUtils.toUTF8Bytes("ENC"));
            md.update(sharedSecret);
            byte[] encryptionSecret = md.digest();
            
            SecretKey macKey = new SecretKeySpec(macSecret, MAC_ALGO);
            SecretKey encryptionKey = new SecretKeySpec(encryptionSecret, ENCRYPTION_KEY_ALGO);
            RemoteIdKeys rpe = new RemoteIdKeys(remoteId, pk, macKey, encryptionKey);        
            return rpe;
        }catch(NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#sign(byte[])
     */
    public byte[] sign(byte[] data){
        try {
            Signature signer = Signature.getInstance(SIG_ALGO);
            signer.initSign(localIdentity.getPrivateSignatureKey());
            byte[] signature = null;
            signer.update(data);
            signature = signer.sign();
            return signature;
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    private byte[] sign(byte[] data, PrivateKey sigKey){
        try {
            Signature signer = Signature.getInstance(SIG_ALGO);
            signer.initSign(sigKey);
            byte[] signature = null;
            signer.update(data);
            signature = signer.sign();
            return signature;
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }    
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#getMyId()
     */
    public GUID getLocalGuid(){
        return localIdentity.getGuid();
    }
    
    private void initDHParamSpec() throws NoSuchAlgorithmException, IOException, InvalidParameterSpecException{
        AlgorithmParameters dhPara = AlgorithmParameters.getInstance(AGREEMENT_ALGO);
        dhPara.init(Base32.decode(DH_PARAMETER));
        dhParamSpec = dhPara.getParameterSpec(DHParameterSpec.class);
    }
    
    private KeyPair signatureKeyPairGen() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(SIG_KEY_ALGO);
        gen.initialize(SIGNATURE_KEY_SIZE, new SecureRandom());
        return gen.generateKeyPair();
    }
    
    private KeyPair dhKeyPairGen() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(AGREEMENT_ALGO);
        
        try {
            gen.initialize(dhParamSpec);
        } catch (InvalidAlgorithmParameterException e) {
        }
        KeyPair keyPair = gen.generateKeyPair();
        return keyPair;
    }
    
    private PrivateIdentityImpl createPrivateIdentity() {
        // multiple installation mark
        int multiInstallationMark = SecurityUtils.createSecureRandomNoBlock().nextInt();
        try {
            // signature key pair
            KeyPair signatureKeyPair = signatureKeyPairGen();        
            // DH key
            KeyPair dhKeyPair = dhKeyPairGen();
            // my GUID
            MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
            md.update(signatureKeyPair.getPublic().getEncoded());
            byte[] hash = md.digest();
            GUID myGuid = new GUID(GUID.makeSecureGuid(hash, multiInstallationMark));
            // signature 
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                out.write(myGuid.bytes());
                out.write(signatureKeyPair.getPublic().getEncoded());
                out.write(((DHPublicKey)dhKeyPair.getPublic()).getY().toByteArray());
            } catch (IOException e) {
                return null;
            }
            byte[] payload = out.toByteArray();
            final byte[] signature = sign(payload, signatureKeyPair.getPrivate());
            
            return new PrivateIdentityImpl(myGuid, signatureKeyPair.getPublic(), ((DHPublicKey)dhKeyPair.getPublic()).getY(), signature, 
                    signatureKeyPair.getPrivate(), (DHPrivateKey)dhKeyPair.getPrivate(), multiInstallationMark);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
            
    private RemoteIdKeys verifyIdentityAndDoKeyAgreement(Identity identity) {
        PublicKey remoteSignatureKey = identity.getPublicSignatureKey();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(identity.getGuid().bytes());
            out.write(identity.getPublicSignatureKey().getEncoded());
            out.write(identity.getPublicDiffieHellmanComponent().toByteArray());
        } catch (IOException e) {
            return null;
        }
        byte[] payload = out.toByteArray();
        
        // verify signature
        if (! verifySignature(remoteSignatureKey, payload, identity.getSignature()))
            return null;

        // verify remoteID
        GUID remoteID = identity.getGuid();//new GUID(remoteGuidBytes);
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(HASH_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(remoteSignatureKey.getEncoded());
        byte[] hash = md.digest();
        
        if (! GUID.isSecureGuid(hash, remoteID) )
            return null;
        
        // diffie-hellman agreement
        DHPublicKeySpec dhPubSpec = new DHPublicKeySpec(identity.getPublicDiffieHellmanComponent(), dhParamSpec.getP(), dhParamSpec.getG());
        KeyAgreement keyAgree;
        try {
            keyAgree = KeyAgreement.getInstance(AGREEMENT_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        KeyFactory factory;
        try {
            factory = KeyFactory.getInstance(AGREEMENT_ALGO);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        PublicKey remoteDhKey;
        try {
            remoteDhKey = factory.generatePublic(dhPubSpec);
            keyAgree.init(localIdentity.getPrivateDiffieHellmanKey());
            keyAgree.doPhase(remoteDhKey, true);
        } catch (InvalidKeySpecException e) {
            return null;
        } catch (InvalidKeyException e) {
            return null;
        } catch (IllegalStateException e) {
            return null;
        }
        
        byte[] sharedSecret = keyAgree.generateSecret();
        RemoteIdKeys rpe = createRemoteIdKeys(remoteID, remoteSignatureKey, sharedSecret);
        return rpe;
    }
}
