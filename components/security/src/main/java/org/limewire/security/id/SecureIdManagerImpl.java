package org.limewire.security.id;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.HashMap;

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

import org.limewire.core.settings.SecuritySettings;
import org.limewire.io.GUID;
import org.limewire.security.SecurityUtils;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;

public class SecureIdManagerImpl implements SecureIdManager {    
    /** 768-bit system-wide Diffie-Hellman parameters
     *  limewire base32 encoded    
     */
    private static final String DH_PARAMETER ="GCA4SATBACPJOPGPJPLAVQ5M37DLZKJ2MJOL5JFGQWUVF52ZEAZSNXJXWVCC7NVGWGVVEEPZBRLQ3GDOP7QZZDD2TP76T2D73UFCM3EYLSTBVYCV2FZNU7CVVQXM32YWZIM3FFJHC22EEED66XIVNHUHXF4TVH335RIQEYC732YJHTFBSNUNT4DJLCZT3NQBE564S76Y7FOOA5W7NTTWWTNX2GZLDNAFH4QKZDQ7NHZQJTKD4X4VULJP66EYLBRXZUCJFZRTRVNM4LLLAX3HEGRYFQ5GYJR73MDJGCQVKBJKZFC26OKTN325OWFD63DTAIBAF7Y";
    
    private PublicKey mySignaturePublicKey;
    private PrivateKey mySignaturePrivateKey;
    private DHPublicKey myDHPublicKey;
    private DHPrivateKey myDHPrivateKey;
    private GUID myGuid;
    private DHParameterSpec dhParamSpec;
    private byte[] multiInstallationMark;
    private HashMap<GUID, RemotePublicKeyEntry> remoteKeys;
    private PrintStream remoteKeysFilePrintStream;
    private Identity localIdentity;
    private boolean writeRemoteKeysToFile = false;
    private String remoteKeysFilename;
    
    public SecureIdManagerImpl(String remoteKeysFilename) {
        // init parameters
        remoteKeys = new HashMap<GUID, RemotePublicKeyEntry>();
        this.remoteKeysFilename = remoteKeysFilename;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#start()
     */
    public void start() throws NoSuchAlgorithmException, InvalidParameterSpecException, IOException{
        // init DH community parameter
        dhParamSpecInit();
        // load my keys, 
        try{
            loadKeys();
        }catch(Exception e){
            genAndSetMyKeys();
        }
        myGuid = generateID();
        localIdentity = createIdentity();
        // init RemoteKeyStorage
        initRemoteKeyStorage(remoteKeysFilename);        
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#stop()
     */
    public void stop(){
        if(writeRemoteKeysToFile){
            remoteKeysFilePrintStream.close();
        }
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#exist(org.limewire.io.GUID)
     */
    public synchronized boolean exist(GUID remoteID){
        return remoteKeys.containsKey(remoteID);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#createHmac(org.limewire.io.GUID, byte[])
     */
    public byte[] createHmac(GUID remoteID, byte[] data) throws NoSuchAlgorithmException{
        if(! exist(remoteID)){
            return null;
        }
        Mac mac = Mac.getInstance(MAC_ALGO);
        try {
            mac.init(getRemotePublicKeyEntry(remoteID).macKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }        
        return mac.doFinal(data);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#verifyHmac(org.limewire.io.GUID, byte[], byte[])
     */
    public boolean verifyHmac(GUID remoteId, byte[] data, byte[] hmacValue) throws NoSuchAlgorithmException {
        if(! exist(remoteId)){
            return false;
        }
        Mac mac = Mac.getInstance(MAC_ALGO);
        try {
            mac.init(getRemotePublicKeyEntry(remoteId).macKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }        
        return Arrays.equals(mac.doFinal(data), hmacValue);
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#encrypt(org.limewire.io.GUID, byte[])
     */
    public byte[] encrypt(GUID remoteId, byte[] plaintext) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        if(! exist(remoteId)){
            return null;
        }
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, remoteKeys.get(remoteId).encryptionKey);
        byte[] encrypted = cipher.doFinal(plaintext);
        //byte[] iv = cipher.getIV();
        //System.out.println("iv: "+new String(iv));
        return encrypted;
    }

    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#decrypt(org.limewire.io.GUID, byte[])
     */
    public byte[] decrypt(GUID remoteId, byte[] ciphertext) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{
        if(! exist(remoteId)){
            return null;
        }
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, remoteKeys.get(remoteId).encryptionKey);
        byte[] plaintext = cipher.doFinal(ciphertext);
        return plaintext;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#verifySignature(org.limewire.io.GUID, byte[], byte[])
     */
    public boolean verifySignature(GUID remoteId, byte [] data, byte [] signature) throws NoSuchAlgorithmException{
        if(! exist(remoteId)){
            return false;
        }
        Signature verifier = Signature.getInstance(SIG_ALGO);
        try {
            verifier.initVerify(getRemotePublicKeyEntry(remoteId).signaturePublicKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        boolean goodSig = false;
        try {
            verifier.update(data);
            goodSig = verifier.verify(signature);
        } catch (SignatureException e) {
            goodSig = false;
        }
        return goodSig;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#getMyIdentity()
     */
    public Identity getLocalIdentity(){
        return localIdentity;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#processIdentity(org.limewire.security.id.Identity)
     */
    public boolean processIdentity(Identity identity) throws NoSuchAlgorithmException{
        RemotePublicKeyEntry rpe = verifyIdentityAndDoKeyAgreement(identity);
        if(rpe == null){// bad identify
            return false;
        }else{
            if( ! exist(identity.getGuid()) ){
                store(identity.getGuid(), rpe);
            }
            return true;
        }
    }

    private RemotePublicKeyEntry createRemotePublicKeyEntry(PublicKey pk, byte[] sharedSecret) throws NoSuchAlgorithmException{
        // generate all the symmetric keys
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
        RemotePublicKeyEntry rpe = new RemotePublicKeyEntry(pk, macKey, encryptionKey);        
        return rpe;
    }
    
    /**
     * store a remote node's public key, Diffie-Hellman public exponent, and shared key 
     */
    private void store(GUID id, RemotePublicKeyEntry rpe){
        putRemotePublicKeyEntry(id, rpe);
        if(writeRemoteKeysToFile){            
            writeEntry(id, rpe);
        }
    }    
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#verifySignature(byte[], byte[], java.security.PublicKey)
     */
    public boolean verifySignature(byte [] data, byte [] signature, PublicKey publicKey) throws NoSuchAlgorithmException {
        Signature verifier = Signature.getInstance(SIG_ALGO);
        try {
            verifier.initVerify(publicKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        boolean goodSig = false;
        
        try {
            verifier.update(data);
            goodSig = verifier.verify(signature);
        } catch (SignatureException e) {
            goodSig = false;
        }
        return goodSig;        
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#sign(byte[])
     */
    public byte[] sign(byte[] data) throws NoSuchAlgorithmException{
        Signature signer = Signature.getInstance(SIG_ALGO);
        try {
            signer.initSign(mySignaturePrivateKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        byte[] signature = null;
        try {
            signer.update(data);
            signature = signer.sign();
        } catch (SignatureException e) {
            return signature;
        }
        return signature;
    }
    
    /* (non-Javadoc)
     * @see org.limewire.security.id.SecureIdManagerI#getMyId()
     */
    public GUID getLocalId(){
        return myGuid;
    }
    
    /** 
     * load remote keys we learned from previous sessions
     * also open remote keys storage file for appending
     * @throws IOException 
     * @throws NoSuchAlgorithmException 
     */
    private void initRemoteKeyStorage(String remoteKeysFilename) throws NoSuchAlgorithmException, IOException {
        if(remoteKeysFilename == null)
            remoteKeysFilename = "remoteKeys.txt";
        
        // load public key list        
        loadRemoteKeys(remoteKeysFilename);       
        // remote keys' file
        remoteKeysFilePrintStream = new PrintStream(new FileOutputStream(remoteKeysFilename, true));
        writeRemoteKeysToFile = true;
    }
    
    private void loadRemoteKeys(String remoteKeysFilename) throws NoSuchAlgorithmException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(remoteKeysFilename)));
        String line = null;
        while ((line = reader.readLine()) != null) {
            try {
                parseEntry(line);
            } catch (InvalidKeySpecException e) {
                // TODO delete line?
            }
        }
        reader.close();
    }
    
    private void loadKeys() throws NoSuchAlgorithmException, InvalidKeySpecException{
        // signature key
        byte[] publicKeyBytes = Base32.decode(SecuritySettings.SIGNATURE_PUBLIC_KEY.get());
        byte[] privateKeyBytes = Base32.decode(SecuritySettings.SIGNATURE_PRIVATE_KEY.get());
        KeyFactory factory = KeyFactory.getInstance(SIG_KEY_ALGO);        
        mySignaturePrivateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        mySignaturePublicKey = factory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        // DH key
        publicKeyBytes = Base32.decode(SecuritySettings.DH_PUBLIC_KEY.get());
        privateKeyBytes = Base32.decode(SecuritySettings.DH_PRIVATE_KEY.get());        
        factory = KeyFactory.getInstance("DH");        
        myDHPrivateKey = (DHPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        myDHPublicKey = (DHPublicKey) factory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));        
        // multiple installation mark
        multiInstallationMark = Base32.decode(SecuritySettings.MULTI_INSTALLATION_MARK.get());        
    }
    
    private void genAndSetMyKeys() throws NoSuchAlgorithmException {
        // signature key
        KeyPair signatureKeyPair = signatureKeyPairGen();        
        mySignaturePublicKey = signatureKeyPair.getPublic();
        mySignaturePrivateKey = signatureKeyPair.getPrivate();
        // DH key
        KeyPair dhKeyPair = dhKeyPairGen();
        myDHPublicKey = (DHPublicKey) dhKeyPair.getPublic();
        myDHPrivateKey = (DHPrivateKey) dhKeyPair.getPrivate();
        // multiple installation mark
        multiInstallationMark = Arrays.copyOf(SecurityUtils.createNonce(), 3);
        // set them, so that they will be stored 
        SecuritySettings.SIGNATURE_PUBLIC_KEY.set(Base32.encode(mySignaturePublicKey.getEncoded()));
        SecuritySettings.SIGNATURE_PRIVATE_KEY.set(Base32.encode(mySignaturePrivateKey.getEncoded()));
        SecuritySettings.DH_PUBLIC_KEY.set(Base32.encode(myDHPublicKey.getEncoded()));
        SecuritySettings.DH_PRIVATE_KEY.set(Base32.encode(myDHPrivateKey.getEncoded()));
        SecuritySettings.MULTI_INSTALLATION_MARK.set(Base32.encode(multiInstallationMark));
    }
    
    private void dhParamSpecInit() throws NoSuchAlgorithmException, IOException, InvalidParameterSpecException{
        AlgorithmParameters dhPara = AlgorithmParameters.getInstance("DH");
        dhPara.init(Base32.decode(DH_PARAMETER));
        dhParamSpec = dhPara.getParameterSpec(DHParameterSpec.class);
    }
    
    private KeyPair signatureKeyPairGen() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(SIG_KEY_ALGO);
        gen.initialize(SIGNATURE_KEY_SIZE, new SecureRandom());
        return gen.generateKeyPair();
    }
    
    private KeyPair dhKeyPairGen() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("DH");
        
        try {
            gen.initialize(dhParamSpec);
        } catch (InvalidAlgorithmParameterException e) {
        }
        KeyPair keyPair = gen.generateKeyPair();
        return keyPair;
    }
    
    private GUID generateID() throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
        
        md.update(mySignaturePublicKey.getEncoded());
        
        byte[] hash = md.digest();
        GUID secureID = new GUID(GUID.makeSecureGuid(multiInstallationMark, hash, TAGGING));
        return secureID;
    }    

    private void writeEntry(GUID id, RemotePublicKeyEntry entry) {        
        String toWrite = Base32.encode(id.bytes()) +"|"+
            Base32.encode(entry.signaturePublicKey.getEncoded()) +"|"+
            Base32.encode(entry.macKey.getEncoded()) +"|"+
            Base32.encode(entry.encryptionKey.getEncoded());        
        remoteKeysFilePrintStream.println(toWrite);
    }
    
    private void parseEntry(String line) throws NoSuchAlgorithmException, InvalidKeySpecException {
        int separater1 = line.indexOf("|");
        int separater2 = line.indexOf("|",separater1+1);        
        int separater3 = line.indexOf("|",separater2+1);
    
        if(separater1 <= 0 || separater2 <= 0 || separater3 <= 0 )
            return;
        
        byte[] guidBytes = Base32.decode(line.substring(0, separater1));
        byte[] signatureKeyBytes = Base32.decode(line.substring(separater1+1, separater2));
        byte[] macKeyBytes = Base32.decode(line.substring(separater2+1, separater3));
        byte[] encryptionKeyBytes = Base32.decode(line.substring(separater3+1));
        
        GUID remoteID = new GUID(guidBytes);
        KeyFactory factory = KeyFactory.getInstance(SIG_KEY_ALGO);
        PublicKey remoteSignatureKey = factory.generatePublic(new X509EncodedKeySpec(signatureKeyBytes));
        SecretKey mackey = new SecretKeySpec(macKeyBytes, MAC_ALGO);
        SecretKey enckey = new SecretKeySpec(encryptionKeyBytes, ENCRYPTION_KEY_ALGO);
        
        RemotePublicKeyEntry entry = new RemotePublicKeyEntry(remoteSignatureKey, mackey, enckey);        
        putRemotePublicKeyEntry(remoteID, entry);
    }
    
    private Identity createIdentity() throws NoSuchAlgorithmException{       
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(myGuid.bytes());
            out.write(mySignaturePublicKey.getEncoded());
            out.write(myDHPublicKey.getY().toByteArray());
        } catch (IOException e) {
            return null;
        }
        byte[] payload = out.toByteArray();
        final byte[] signature = sign(payload);
        
        return new IdentityImpl(myGuid, mySignaturePublicKey, myDHPublicKey.getY(), signature);
    }
            
    private RemotePublicKeyEntry verifyIdentityAndDoKeyAgreement(Identity identity) throws NoSuchAlgorithmException{
        PublicKey remoteSignatureKey = identity.getSignatureKey();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(identity.getGuid().bytes());
            out.write(identity.getSignatureKey().getEncoded());
            out.write(identity.getDHPublicComponent().toByteArray());
        } catch (IOException e) {
            return null;
        }
        byte[] payload = out.toByteArray();
        
        // verify signature
        boolean goodSig = verifySignature(payload, identity.getSignature(), remoteSignatureKey);
        if (! goodSig )
            return null;

        // verify remoteID
        GUID remoteID = identity.getGuid();//new GUID(remoteGuidBytes);
        MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
        md.update(remoteSignatureKey.getEncoded());
        byte[] hash = md.digest();
        
        if (! GUID.isSecureGuid(hash, remoteID, TAGGING) )
            return null;
        
        // diffie-hellman agreement
        DHPublicKeySpec dhPubSpec = new DHPublicKeySpec(identity.getDHPublicComponent(), dhParamSpec.getP(), dhParamSpec.getG());
        KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
        KeyFactory factory = KeyFactory.getInstance("DH");
        PublicKey remoteDhKey;
        try {
            remoteDhKey = factory.generatePublic(dhPubSpec);
            keyAgree.init(myDHPrivateKey);
            keyAgree.doPhase(remoteDhKey, true);
        } catch (InvalidKeySpecException e) {
            return null;
        } catch (InvalidKeyException e) {
            return null;
        } catch (IllegalStateException e) {
            return null;
        }
        
        byte[] sharedSecret = keyAgree.generateSecret();
        RemotePublicKeyEntry rpe = createRemotePublicKeyEntry(remoteSignatureKey, sharedSecret);
        return rpe;
    }
    
    protected synchronized void putRemotePublicKeyEntry(GUID id, RemotePublicKeyEntry rpe){
        remoteKeys.put(id, rpe);
    }
    
    protected synchronized RemotePublicKeyEntry getRemotePublicKeyEntry(GUID id){
        return remoteKeys.get(id);
    }
    
    class RemotePublicKeyEntry{
        PublicKey signaturePublicKey;        
        SecretKey macKey;
        SecretKey encryptionKey;
        
        public RemotePublicKeyEntry(PublicKey pk, SecretKey hmacKey, SecretKey encKey) {
            signaturePublicKey = pk;
            macKey = hmacKey;
            encryptionKey = encKey;
        }       
    }
}
