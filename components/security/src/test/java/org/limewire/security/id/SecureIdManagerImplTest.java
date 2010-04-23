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
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        aliceIdManager.start();
        
        MessageDigest md = MessageDigest.getInstance(SecureIdManager.HASH_ALGO);
        md.update(aliceIdManager.getPublicLocalIdentity().getPublicSignatureKey().getEncoded());
        byte[] hash = md.digest();
        assertTrue(GUID.isSecureGuid(hash, aliceIdManager.getLocalGuid()));        
    }
    
    public void testAddIdentity() throws Exception{
        // the two parties of key agreement: alice and bob
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        aliceIdManager.start();
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());        
        bobIdManager.start();
        // alice and bob exchange identities
        Identity request = aliceIdManager.getPublicLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getPublicLocalIdentity();
        aliceIdManager.addIdentity(reply);
        
        // alice and bob have each other in their store
        // alice and bob share same mac and encryption keys
        assertEquals(
                (new RemoteIdKeys(aliceIdManager.getSecureIdStore().get(bobIdManager.getLocalGuid()))).getEncryptionKey().getEncoded(), 
                (new RemoteIdKeys(bobIdManager.getSecureIdStore().get(aliceIdManager.getLocalGuid()))).getEncryptionKey().getEncoded() );
        assertEquals(
                (new RemoteIdKeys(aliceIdManager.getSecureIdStore().get(bobIdManager.getLocalGuid()))).getMacKey().getEncoded(), 
                (new RemoteIdKeys(bobIdManager.getSecureIdStore().get(aliceIdManager.getLocalGuid()))).getMacKey().getEncoded() );
        
        // make identity with wrong id, key, signature, so addidentity will fail
        Identity bobIdentity = bobIdManager.getPublicLocalIdentity();
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
        
        assertFalse(aliceIdManager.addIdentity(new IdentityImpl(wrongId, goodSigKey, goodDHPC, goodSig)));
        assertFalse(aliceIdManager.addIdentity(new IdentityImpl(goodId, wrongSigKey, goodDHPC, goodSig)));
        assertFalse(aliceIdManager.addIdentity(new IdentityImpl(goodId, goodSigKey, wrongDHPC, goodSig)));
        assertFalse(aliceIdManager.addIdentity(new IdentityImpl(goodId, goodSigKey, goodDHPC, wrongSig)));
    }

    public void testIsKnown() throws Exception{
        // alice and bob and cara
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        aliceIdManager.start();
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());        
        bobIdManager.start();
        SecureIdManagerImpl caraIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        caraIdManager.start();
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getPublicLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getPublicLocalIdentity();
        aliceIdManager.addIdentity(reply);
        // alice and cara exchange identities
        caraIdManager.addIdentity(request);
        reply = caraIdManager.getPublicLocalIdentity();
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
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        aliceIdManager.start();
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());        
        bobIdManager.start();
        SecureIdManagerImpl caraIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        caraIdManager.start();
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getPublicLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getPublicLocalIdentity();
        aliceIdManager.addIdentity(reply);
        // alice and cara exchange identities
        caraIdManager.addIdentity(request);
        reply = caraIdManager.getPublicLocalIdentity();
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
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        aliceIdManager.start();
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());        
        bobIdManager.start();
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getPublicLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getPublicLocalIdentity();
        aliceIdManager.addIdentity(reply);
        
        // remoteIdKeys should be 196 bytes long. that is used to setup database.
        assertEquals(aliceIdManager.getSecureIdStore().get(bobIdManager.getLocalGuid()).length, 196); 
    }
    
    public void testStartFromStoredIdentity() throws Exception{
        SecureIdStore idStore = new SecureIdStoreImpl();
        assertNull(idStore.getLocalData());
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(idStore);
        aliceIdManager.start();      
        assertNotNull(idStore.getLocalData());
        // clone alice with the privateIdentity
        SecureIdManagerImpl aliceCloneIdManager = new SecureIdManagerImpl(idStore);
        aliceCloneIdManager.start();
        // alice and her clone should have the same identity
        assertEquals(aliceIdManager.getPrivateLocalIdentity().toByteArray(), aliceCloneIdManager.getPrivateLocalIdentity().toByteArray());
    }

    public void testEncryptionAndDecryption() throws Exception{
        // alice and bob and cara
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        aliceIdManager.start();
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());        
        bobIdManager.start();
        SecureIdManagerImpl caraIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        caraIdManager.start();
        // alice and bob exchange identities 
        Identity request = aliceIdManager.getPublicLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getPublicLocalIdentity();
        aliceIdManager.addIdentity(reply);
        // alice and cara exchange identities
        caraIdManager.addIdentity(request);
        reply = caraIdManager.getPublicLocalIdentity();
        aliceIdManager.addIdentity(reply);
                 
        // test encryption
        // alice encrypt
        byte[] plaintext = "alice data".getBytes();
        byte[] ciphertextToBob = aliceIdManager.encrypt(bobIdManager.getLocalGuid(), plaintext);
        byte[] ciphertextToCara = aliceIdManager.encrypt(caraIdManager.getLocalGuid(), plaintext);
        // bob and cara decrypt
        assertEquals(bobIdManager.decrypt(aliceIdManager.getLocalGuid(), ciphertextToBob), plaintext);
        assertEquals(caraIdManager.decrypt(aliceIdManager.getLocalGuid(), ciphertextToCara), plaintext);
        // wont work if sent to wrong receiver
        try{
            bobIdManager.decrypt(aliceIdManager.getLocalGuid(), ciphertextToCara);
            fail("bad ciphertext");
        } catch(Exception e){
        }
    }    
}
