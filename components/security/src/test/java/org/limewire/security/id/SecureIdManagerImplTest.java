package org.limewire.security.id;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;

import junit.framework.Test;

import org.limewire.io.GUID;
import org.limewire.security.SecurityUtils;
import org.limewire.util.Base32;
import org.limewire.util.BaseTestCase;

public class SecureIdManagerImplTest extends BaseTestCase {
    public SecureIdManagerImplTest(String name){
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SecureIdManagerImplTest.class);
    }

    public void testIdGeneration() throws Exception{
        // generate id, test if it is secure id        
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        
        MessageDigest md = MessageDigest.getInstance(SecureIdManager.HASH_ALGO);
        md.update(aliceIdManager.getLocalIdentity().getPublicSignatureKey().getEncoded());
        byte[] hash = md.digest();
        assertTrue(GUID.isSecureGuid(hash, aliceIdManager.getLocalGuid()));        
    }
    
    public void testAddIdentity() throws Exception{
        // the two parties of key agreement: alice and bob
        SimpleSecureIdStoreImpl aliceStorage = new SimpleSecureIdStoreImpl();
        SimpleSecureIdStoreImpl bobStorage = new SimpleSecureIdStoreImpl();
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(aliceStorage);        
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(bobStorage);
        // alice and bob exchange identities
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        
        // alice and bob have each other in their store
        // alice and bob share same mac and encryption keys
        assertEquals(
                (new RemoteIdKeys(aliceStorage.get(bobIdManager.getLocalGuid()))).getOutgoingEncryptionKey().getEncoded(), 
                (new RemoteIdKeys(bobStorage.get(aliceIdManager.getLocalGuid()))).getIncomingDecryptionKey().getEncoded() );
        assertEquals(
                (new RemoteIdKeys(aliceStorage.get(bobIdManager.getLocalGuid()))).getIncomingDecryptionKey().getEncoded(), 
                (new RemoteIdKeys(bobStorage.get(aliceIdManager.getLocalGuid()))).getOutgoingEncryptionKey().getEncoded() );
        assertEquals(
                (new RemoteIdKeys(aliceStorage.get(bobIdManager.getLocalGuid()))).getOutgoingMacHmacKey().getEncoded(), 
                (new RemoteIdKeys(bobStorage.get(aliceIdManager.getLocalGuid()))).getIncomingVerificationHmacKey().getEncoded() );
        assertEquals(
                (new RemoteIdKeys(aliceStorage.get(bobIdManager.getLocalGuid()))).getIncomingVerificationHmacKey().getEncoded(), 
                (new RemoteIdKeys(bobStorage.get(aliceIdManager.getLocalGuid()))).getOutgoingMacHmacKey().getEncoded() );
        
        // make identity with wrong id, key, signature, so addidentity will fail
        Identity bobIdentity = bobIdManager.getLocalIdentity();
        // wrong id
        GUID goodId = bobIdentity.getGuid();
        GUID wrongId = new GUID();
        
        // wrong signature key
        PublicKey goodSigKey = bobIdentity.getPublicSignatureKey();
        KeyPairGenerator gen = KeyPairGenerator.getInstance(SecureIdManager.SIG_KEY_ALGO);
        gen.initialize(SecureIdManager.SIGNATURE_KEY_SIZE, new SecureRandom());
        KeyPair kp = gen.generateKeyPair();
        PublicKey wrongSigKey = kp.getPublic(); 
        
        // wrong diffie-hellman public component
        BigInteger goodDHPC = bobIdentity.getPublicDiffieHellmanComponent();
        BigInteger wrongDHPC = BigInteger.TEN;
        
        // wrong signature 
        byte[] goodSig = bobIdentity.getSignature();
        byte[] wrongSig = goodSig;
        wrongSig[2] += 1;
        
        assertFalse(aliceIdManager.addIdentity(new SimpleIdentityImpl(wrongId, goodSigKey, goodDHPC, goodSig)));
        assertFalse(aliceIdManager.addIdentity(new SimpleIdentityImpl(goodId, wrongSigKey, goodDHPC, goodSig)));
        assertFalse(aliceIdManager.addIdentity(new SimpleIdentityImpl(goodId, goodSigKey, wrongDHPC, goodSig)));
        assertFalse(aliceIdManager.addIdentity(new SimpleIdentityImpl(goodId, goodSigKey, goodDHPC, wrongSig)));
    }

    public void testIsKnown() throws Exception{
        // alice and bob and cara
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());        
        SecureIdManagerImpl caraIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        // alice and cara exchange identities
        caraIdManager.addIdentity(request);
        reply = caraIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        
        // alice and bob know each other 
        assertTrue(aliceIdManager.isKnown(bobIdManager.getLocalGuid()));
        assertTrue(bobIdManager.isKnown(aliceIdManager.getLocalGuid()));
        // alice and cara know each other
        assertTrue(aliceIdManager.isKnown(caraIdManager.getLocalGuid()));
        assertTrue(caraIdManager.isKnown(aliceIdManager.getLocalGuid()));
        // bob and cara don't know each other
        assertFalse(caraIdManager.isKnown(bobIdManager.getLocalGuid()));
        assertFalse(bobIdManager.isKnown(caraIdManager.getLocalGuid()));
    }

    public void testSignatureAndHmac() throws Exception {
        // alice and bob and cara
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());        
        SecureIdManagerImpl caraIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        // alice and cara exchange identities
        caraIdManager.addIdentity(request);
        reply = caraIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
                 
        // test signature stuff
        // alice generates a signature 
        byte[] data = "alice data".getBytes();
        byte[] sigBytes = aliceIdManager.sign(data);
        // bob and cara somehow knows that the signature are from alice
        GUID aliceId = aliceIdManager.getLocalGuid();   
        // bob and cara verify the signature
        assertTrue(bobIdManager.verifySignature(aliceId, data, sigBytes));
        assertTrue(caraIdManager.verifySignature(aliceId, data, sigBytes));
        // wont verify if the id is wrong
        try{
            GUID caraId = caraIdManager.getLocalGuid();
            bobIdManager.verifySignature(caraId, data, sigBytes);
            fail("unknown ID "+caraId);
        } catch(Exception e){
        }
        
        // alice generates mac for bob and cara
        GUID bobId = bobIdManager.getLocalGuid();
        GUID caraId = caraIdManager.getLocalGuid();
        byte[] macBytesToBob = aliceIdManager.createHmac(bobId, data);
        byte[] macBytesToCara = aliceIdManager.createHmac(caraId, data);
        // bob and cara verify the mac
        assertTrue(bobIdManager.verifyHmac(aliceId, data, macBytesToBob));
        assertTrue(caraIdManager.verifyHmac(aliceId, data, macBytesToCara));
        // wont verify if the ids are wrong
        assertFalse(bobIdManager.verifyHmac(aliceId, data, macBytesToCara));
        assertFalse(caraIdManager.verifyHmac(aliceId, data, macBytesToBob));
        
        // bad signature or mac should fail
        byte[] badSigBytes = sigBytes;
        byte[] badMacBytes = macBytesToBob;
        badSigBytes[10] += 1;
        badMacBytes[10] += 1;
        assertFalse(bobIdManager.verifySignature(aliceId, data, badSigBytes));        
        assertFalse(bobIdManager.verifyHmac(aliceId, data, badMacBytes));
        // bad signature or mac with wrong length should fail, but should not throw exception
        assertFalse(bobIdManager.verifySignature(aliceId, data, SecurityUtils.createNonce()));
        assertFalse(bobIdManager.verifyHmac(aliceId, data, SecurityUtils.createNonce()));
    }   
    
    public void testRemoteKeyLength() throws Exception {
        SimpleSecureIdStoreImpl aliceStorage = new SimpleSecureIdStoreImpl();
        SimpleSecureIdStoreImpl bobStorage = new SimpleSecureIdStoreImpl();
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(aliceStorage);        
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(bobStorage);
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        
        // remoteIdKeys should be 249 bytes long. that is used to setup database.
        assertLessThanOrEquals(aliceStorage.get(bobIdManager.getLocalGuid()).length, 249); 
        assertLessThanOrEquals(bobStorage.get(aliceIdManager.getLocalGuid()).length, 249); 
    }
    
    public void testStartFromStoredIdentity() throws Exception{
        SecureIdStore idStore = new SimpleSecureIdStoreImpl();
        assertNull(idStore.getLocalData());
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(idStore);
        assertNotNull(idStore.getLocalData());
        // clone alice with the privateIdentity
        SecureIdManagerImpl aliceCloneIdManager = new SecureIdManagerImpl(idStore);
        // alice and her clone should have the same identity
        assertEquals(((PrivateIdentity)aliceIdManager.getLocalIdentity()).toByteArray(), ((PrivateIdentity)aliceCloneIdManager.getLocalIdentity()).toByteArray());
    }

    public void testEncryptionAndDecryption() throws Exception{
        // alice and bob and cara
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());        
        SecureIdManagerImpl caraIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        // alice and cara exchange identities
        caraIdManager.addIdentity(request);
        reply = caraIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
                 
        // test encryption
        // alice encrypt
        byte[] plaintext = "alice data".getBytes();
        byte[] randomeIvBytes = (new GUID()).bytes();
        byte[] ciphertextToBob = aliceIdManager.encrypt(bobIdManager.getLocalGuid(), plaintext, randomeIvBytes);
        byte[] ciphertextToCara = aliceIdManager.encrypt(caraIdManager.getLocalGuid(), plaintext, randomeIvBytes);
        // bob and cara decrypt
        assertEquals(bobIdManager.decrypt(aliceIdManager.getLocalGuid(), ciphertextToBob, randomeIvBytes), plaintext);
        assertEquals(caraIdManager.decrypt(aliceIdManager.getLocalGuid(), ciphertextToCara, randomeIvBytes), plaintext);
        // won't work if sent to wrong receiver
        try{
            bobIdManager.decrypt(aliceIdManager.getLocalGuid(), ciphertextToCara, randomeIvBytes);
            fail("bad ciphertext");
        } catch(Exception e){
        }
        // won't work if IV is not 16 bytes
        try{
            randomeIvBytes = SecurityUtils.createNonce();
            ciphertextToBob = aliceIdManager.encrypt(bobIdManager.getLocalGuid(), plaintext, randomeIvBytes);
            fail("Invalid IV");
        } catch(Exception e){
        }
    }  
    
    /**
     * This is an example of secure communication between two parties. 
     * They exchange their identity information, do key agreement, 
     * then authenticate and encrypt their messages. 
     */
    public void testAliceAndBobSecureCommunication() throws Exception{
        // alice
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        // bob
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());          
        
        // alice and bob exchange their identity information and do key agreement
        // alice generates a request which is basically an identity
        Identity request = aliceIdManager.getLocalIdentity();
        // bob process the request
        boolean goodRequest= bobIdManager.addIdentity(request);
        assertTrue(goodRequest);
        // alice's request looks good, gonna reply.
        Identity reply = bobIdManager.getLocalIdentity();
        // alice process the reply
        aliceIdManager.addIdentity(reply);
        //Now, alice and bob know each other and share a key.
         
        // authenticate and encrypt their messages
        // example of authenticated message reply
        // alice generates a message with a nonce        
        // alice asks "who has a blowfish book?"
        String requestData = "who has a blowfish book?";
        byte[] challengeStoredLocally = SecurityUtils.createNonce();
        String message = Base32.encode(aliceIdManager.getLocalGuid().bytes()) +"|"+ requestData +"|"+ Base32.encode(challengeStoredLocally);            
        // somehow bob receives the message and processes it
        int separater1 = message.indexOf("|");
        int separater2 = message.indexOf("|",separater1+1);
        GUID requesterGUID = new GUID(Base32.decode(message.substring(0, separater1)));        
        String alicesChallenge = message.substring(separater2+1);
        String replyData = "bob has a blowfish spec!";        
        String toSign = Base32.encode(bobIdManager.getLocalGuid().bytes()) +"|"+ replyData +"|"+ alicesChallenge;
        // bob does both signature and mac for fun
        String sigStr = Base32.encode(bobIdManager.sign(toSign.getBytes()));
        String macStr = Base32.encode(bobIdManager.createHmac(requesterGUID, toSign.getBytes()));
        String signedMessageReply = toSign +"|"+ sigStr; 
        String macedMessageReply = toSign +"|"+ macStr;

        // alice gets the replies and process them
        /* (I) process the signed reply */
        // 1) parse the reply
        separater1 = signedMessageReply.indexOf("|");
        separater2 = signedMessageReply.indexOf("|",separater1+1);
        int separater3 = signedMessageReply.indexOf("|",separater2+1);
        GUID replierGUID = new GUID(Base32.decode(signedMessageReply.substring(0, separater1)));
        byte[] sig = Base32.decode(signedMessageReply.substring(separater3));
        byte[] challengeFormMessage = Base32.decode(signedMessageReply.substring(separater2+1, separater3));
        byte[] toVerify = signedMessageReply.substring(0, separater3).getBytes();
        String replyStr = signedMessageReply.substring(separater1+1, separater2);
        // 2) verify the challenge 
        assertEquals(challengeStoredLocally, challengeFormMessage);
        // 3) verify the signature
        assertTrue(aliceIdManager.verifySignature(replierGUID, toVerify, sig));
        // 4) use the reply
        assertEquals(replyStr, replyData);
        /* (II) process the maced reply */
        // 1) parse the reply
        separater1 = macedMessageReply.indexOf("|");
        separater2 = macedMessageReply.indexOf("|",separater1+1);
        separater3 = macedMessageReply.indexOf("|",separater2+1);
        replierGUID = new GUID(Base32.decode(macedMessageReply.substring(0, separater1)));
        byte[] mac = Base32.decode(macedMessageReply.substring(separater3));
        challengeFormMessage = Base32.decode(macedMessageReply.substring(separater2+1, separater3));
        toVerify = macedMessageReply.substring(0, separater3).getBytes();
        replyStr = macedMessageReply.substring(separater1+1, separater2);
        // 2) verify the challenge 
        assertEquals(challengeStoredLocally, challengeFormMessage);
        // 3) verify the mac
        assertTrue(aliceIdManager.verifyHmac(replierGUID, toVerify, mac));
        // 4) use the reply
        assertEquals(replyStr, replyData);
            
        // alice and bob use encrypted communication
        // 1) alice encrypts something
        String plaintext = "plaintext: Encryption is the process of converting normal data or plaintext to something incomprehensible or cipher-text by applying mathematical transformations.";
        byte[] plaintextBytes = plaintext.getBytes();
        GUID bobID = bobIdManager.getLocalGuid();
        GUID aliceID = aliceIdManager.getLocalGuid();
        byte[] ciphertextBytes = aliceIdManager.encrypt(bobID, plaintextBytes, bobID.bytes());
        // 2) bob decrypts 
        byte[] bobPlaintextBytes = bobIdManager.decrypt(aliceID, ciphertextBytes, bobID.bytes());
        assertEquals(plaintext, new String(bobPlaintextBytes));
    }
    
    class SimpleIdentityImpl implements Identity{
        private GUID id;
        private PublicKey signaturePublicKey;
        private BigInteger dhPublicComponent;
        private byte[] signature;
        
        public SimpleIdentityImpl(GUID id, PublicKey signatureKey, BigInteger dhPublicComponent, byte[] signature){
            this.id = id;
            this.signaturePublicKey = signatureKey;
            this.dhPublicComponent = dhPublicComponent;
            this.signature = signature;
        }
        
        @Override
        public GUID getGuid() {
            return id;
        }

        @Override
        public BigInteger getPublicDiffieHellmanComponent() {
            return dhPublicComponent;
        }

        @Override
        public PublicKey getPublicSignatureKey() {
            return signaturePublicKey;
        }

        @Override
        public byte[] getSignature() {
            return signature;
        }
    }
}
