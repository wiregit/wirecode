package org.limewire.security.id;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
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

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.limewire.core.settings.SecuritySettings;
import org.limewire.io.GUID;
import org.limewire.util.Base32;
import org.limewire.util.ByteUtils;

public class SecureIdManager {    
    // 768-bit system-wide Diffie-Hellman parameters    
    private static final String DH_PARAMETER ="GCA4SATBACPJOPGPJPLAVQ5M37DLZKJ2MJOL5JFGQWUVF52ZEAZSNXJXWVCC7NVGWGVVEEPZBRLQ3GDOP7QZZDD2TP76T2D73UFCM3EYLSTBVYCV2FZNU7CVVQXM32YWZIM3FFJHC22EEED66XIVNHUHXF4TVH335RIQEYC732YJHTFBSNUNT4DJLCZT3NQBE564S76Y7FOOA5W7NTTWWTNX2GZLDNAFH4QKZDQ7NHZQJTKD4X4VULJP66EYLBRXZUCJFZRTRVNM4LLLAX3HEGRYFQ5GYJR73MDJGCQVKBJKZFC26OKTN325OWFD63DTAIBAF7Y";
    
    private PublicKey mySignaturePublicKey;
    private PrivateKey mySignaturePrivateKey;
    private DHPublicKey myDHPublicKey;
    private DHPrivateKey myDHPrivateKey;
    private GUID myGuid;
    private DHParameterSpec dhParamSpec;
    private SecureRandom rand;    
    private HashMap<GUID, RemotePublicKeyEntry> remoteKeys;
    private PrintStream remoteKeysFilePrintStream;
    private Identity myIdentity;
    private boolean writeRemoteKeysToFile = false;
    
    public static final String SIG_KEY_ALGO = "RSA";
    public static final String SIG_ALGO = "SHA1withRSA";
    public static final String HASH_ALGO = "MD5";
    public static final String MAC_ALGO = "HmacMD5";
    public static final int SIGNATURE_KEY_SIZE = 768;
    
    public SecureIdManager(GUID sturgeonID, String remoteKeysFilename) throws NoSuchAlgorithmException, InvalidParameterSpecException, IOException {
        // init parameters
        rand = new SecureRandom();
        remoteKeys = new HashMap<GUID, RemotePublicKeyEntry>();
        dhParamSpecInit();
        // load my keys, 
        try{
            loadKeys();
        }catch(Exception e){
            genAndSetMyKeys();
        }
        myGuid = generateID(sturgeonID);
        myIdentity = createIdentity();
        initRemoteKeyStorage(remoteKeysFilename);
    }
    
    public void stop(){
        if(writeRemoteKeysToFile){
            remoteKeysFilePrintStream.close();
        }
    }
    
    /**
     * @return a random integer as the nonce
     */    
    public byte[] nonce(){
        byte [] buf = new byte[4];
        ByteUtils.int2beb(rand.nextInt(), buf, 0);
        return buf;
    }
    
    /**
     * @return if the local node knows the remoteID and shares a key with the remote node
     */
    public synchronized boolean exist(GUID remoteID){
        return remoteKeys.containsKey(remoteID);
    }
    
    /**
     * @return hmac value
     * @throws NoSuchAlgorithmException 
     */
    public byte[] hamc(GUID remoteID, byte[] data) throws NoSuchAlgorithmException{
        if(! exist(remoteID)){
            return null;
        }
        Mac mac = Mac.getInstance(MAC_ALGO);
        try {
            mac.init(getRemotePublicKeyEntry(remoteID).sharedKey);
        } catch (InvalidKeyException e) {
            // TODO remove key? 
            return null;
        }        
        return mac.doFinal(data);
    }
    
    /**
     * @return true if the data can be authenticated, i.e., the remoteID generated the hmac using the data.  
     * @throws NoSuchAlgorithmException 
     */
    public boolean verifyHmac(GUID remoteId, byte[] data, byte[] hmacValue) throws NoSuchAlgorithmException {
        if(! exist(remoteId)){
            return false;
        }
        Mac mac = Mac.getInstance(MAC_ALGO);
        try {
            mac.init(getRemotePublicKeyEntry(remoteId).sharedKey);
        } catch (InvalidKeyException e) {
            // TODO remove key? 
            return false; 
        }        
        return Arrays.equals(mac.doFinal(data), hmacValue);
    }
    
    /**
     * @return true if the data can be authenticated, i.e., the remoteID generated the signature using the data.  
     * @throws NoSuchAlgorithmException 
     */
    public boolean verifySignature(GUID remoteId, byte [] data, byte [] signature) throws NoSuchAlgorithmException{
        if(! exist(remoteId)){
            return false;
        }
        Signature verifier = Signature.getInstance(SIG_ALGO);
        try {
            verifier.initVerify(getRemotePublicKeyEntry(remoteId).signaturePublicKey);
        } catch (InvalidKeyException e) {
            // TODO remove key? 
            return false;
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
    
    public Identity getMyIdentity(){
        return myIdentity;
    }
    
    /**
     * process a remote node's identity:
     * 1) verify the remote node's id against its signature public key
     * 2) verify the signature
     * 3) store the identity if it is not in my list
     * @param identity
     * @return true if the remote node's identity is valid based on step 1) and 2). 
     */
    public boolean processIdentity(Identity identity){
        RemotePublicKeyEntry rpe = null;
        try {
            rpe = verifyIdentityAndDoKeyAgreement(identity);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        if(rpe == null){ // bad identify
            return false;
        }else{
            if( ! exist(identity.getGuid()) ){
                store(rpe);
            }
            return true;
        }
    }
    
    /**
     * @return true if the signature is valid  
     * @throws NoSuchAlgorithmException 
     */
    public boolean verifySignature(byte [] data, byte [] signature, PublicKey publicKey) throws NoSuchAlgorithmException {
        Signature verifier = Signature.getInstance(SIG_ALGO);
        try {
            verifier.initVerify(publicKey);
        } catch (InvalidKeyException e) {
            // TODO delete key?             
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
    
    /**
     * @return the signature 
     * @throws NoSuchAlgorithmException 
     */
    public byte[] sign(byte[] data) throws NoSuchAlgorithmException{
        Signature signer = Signature.getInstance(SIG_ALGO);
        try {
            signer.initSign(mySignaturePrivateKey);
        } catch (InvalidKeyException e) {
            // TODO regenerate key?
            return null;
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
    
    public GUID getMyId(){
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
        byte[] publicKeyBytes = Base32.decode(SecuritySettings.SIGNATURE_PUBLIC_KEY.get());
        byte[] privateKeyBytes = Base32.decode(SecuritySettings.SIGNATURE_PRIVATE_KEY.get());
        KeyFactory factory = KeyFactory.getInstance(SIG_KEY_ALGO);        
        mySignaturePrivateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        mySignaturePublicKey = factory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
        
        publicKeyBytes = Base32.decode(SecuritySettings.DH_PUBLIC_KEY.get());
        privateKeyBytes = Base32.decode(SecuritySettings.DH_PRIVATE_KEY.get());        
        factory = KeyFactory.getInstance("DH");        
        myDHPrivateKey = (DHPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
        myDHPublicKey = (DHPublicKey) factory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));        
        
    }
    
    private void genAndSetMyKeys() throws NoSuchAlgorithmException {
        KeyPair signatureKeyPair = signatureKeyPairGen();
        KeyPair dhKeyPair = dhKeyPairGen();
        mySignaturePublicKey = signatureKeyPair.getPublic();
        mySignaturePrivateKey = signatureKeyPair.getPrivate();
        myDHPublicKey = (DHPublicKey) dhKeyPair.getPublic();
        myDHPrivateKey = (DHPrivateKey) dhKeyPair.getPrivate();
        
        SecuritySettings.SIGNATURE_PUBLIC_KEY.set(Base32.encode(mySignaturePublicKey.getEncoded()));
        SecuritySettings.SIGNATURE_PRIVATE_KEY.set(Base32.encode(mySignaturePrivateKey.getEncoded()));
        SecuritySettings.DH_PUBLIC_KEY.set(Base32.encode(myDHPublicKey.getEncoded()));
        SecuritySettings.DH_PRIVATE_KEY.set(Base32.encode(myDHPrivateKey.getEncoded()));        
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
    
    private GUID generateID(GUID sturgeonID) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance(HASH_ALGO);
        
        md.update(mySignaturePublicKey.getEncoded());
        
        byte[] hash = md.digest();
        GUID secureID = new GUID(GUID.makeSecureGuid(sturgeonID, hash));
        return secureID;
    }    

    private void writeEntry(RemotePublicKeyEntry entry) {        
        String toWrite = Base32.encode(entry.id.bytes()) +"|"+
            Base32.encode(entry.signaturePublicKey.getEncoded()) +"|"+
            Base32.encode(entry.dhPublicKey.getEncoded()) +"|"+
            Base32.encode(entry.sharedKey.getEncoded());
        
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
        byte[] dhKeyBytes = Base32.decode(line.substring(separater2+1, separater3));
        byte[] sharedKeyBytes = Base32.decode(line.substring(separater3));
                
        GUID remoteID = new GUID(guidBytes);
        KeyFactory factory = KeyFactory.getInstance(SIG_KEY_ALGO);
        PublicKey remoteSignatureKey = factory.generatePublic(new X509EncodedKeySpec(signatureKeyBytes));
        factory = KeyFactory.getInstance("DH");
        PublicKey remoteDhKey = factory.generatePublic(new X509EncodedKeySpec(dhKeyBytes));
        SecretKey secretkey = new SecretKeySpec(sharedKeyBytes, MAC_ALGO);
        
        RemotePublicKeyEntry entry = new RemotePublicKeyEntry(remoteID, remoteSignatureKey, remoteDhKey, secretkey);        
        putRemotePublicKeyEntry(entry);
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
        
        return new Identity() {
            @Override
            public BigInteger getDHPublicComponent() {
                return myDHPublicKey.getY();
            }

            @Override
            public GUID getGuid() {
                return myGuid;
            }

            @Override
            public PublicKey getSignatureKey() {
                return mySignaturePublicKey;
            }

            @Override
            public byte[] getSignature() {
                return signature;
            }
            
        };
    }
    
    public interface Identity {
        GUID getGuid();
        PublicKey getSignatureKey();
        BigInteger getDHPublicComponent();
        byte[] getSignature();
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
        
        if (! GUID.isSecureGuid(hash, remoteID) )
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
        SecretKey sharedKey = new SecretKeySpec(sharedSecret, MAC_ALGO);
        return new RemotePublicKeyEntry(remoteID, remoteSignatureKey, remoteDhKey, sharedKey);
    }
    
    /**
     * store a remote node's public key, Diffie-Hellman public exponent, and shared key 
     */
    private void store(RemotePublicKeyEntry rpe){
        putRemotePublicKeyEntry(rpe);
        if(writeRemoteKeysToFile){            
            writeEntry(rpe);
        }
    }    
    
    protected synchronized void putRemotePublicKeyEntry(RemotePublicKeyEntry rpe){
        remoteKeys.put(rpe.id, rpe);
    }
    
    protected synchronized RemotePublicKeyEntry getRemotePublicKeyEntry(GUID id){
        return remoteKeys.get(id);
    }
    
    class RemotePublicKeyEntry{
        PublicKey signaturePublicKey;        
        PublicKey dhPublicKey;
        GUID id;
        SecretKey sharedKey;
        
        public RemotePublicKeyEntry(GUID remoteId, PublicKey pk, PublicKey dhpk, SecretKey sk) {
            id = remoteId;
            signaturePublicKey = pk;
            dhPublicKey = dhpk;
            sharedKey = sk;
        }       
    }
}
