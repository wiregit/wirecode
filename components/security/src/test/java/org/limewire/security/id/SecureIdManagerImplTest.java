package org.limewire.security.id;

import java.security.MessageDigest;
import java.util.Arrays;

import junit.framework.Test;

import org.limewire.io.GUID;
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
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(null);
        aliceIdManager.start();
        
        MessageDigest md = MessageDigest.getInstance(SecureIdManager.HASH_ALGO);
        md.update(aliceIdManager.getPublicLocalIdentity().getPublicSignatureKey().getEncoded());
        byte[] hash = md.digest();
        assertTrue(GUID.isSecureGuid(hash, aliceIdManager.getLocalGuid()));        
    }
        
    public void testKeyAgreement() throws Exception{
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
        assertTrue(Arrays.equals(
                (new RemoteIdKeys(aliceIdManager.getSecureIdStore().get(bobIdManager.getLocalGuid()))).getEncryptionKey().getEncoded(), 
                (new RemoteIdKeys(bobIdManager.getSecureIdStore().get(aliceIdManager.getLocalGuid()))).getEncryptionKey().getEncoded() ));
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

    public void testHmac() throws Exception {
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
                 
        // alice generates mac for bob
        byte[] data = "alice data".getBytes();
        GUID bobId = bobIdManager.getLocalGuid();
        byte[] sigBytes = aliceIdManager.sign(data);
        byte[] macBytes = aliceIdManager.createHmac(bobId, data);
        // bob knows that the signature and mac are from alice
        GUID aliceId = aliceIdManager.getLocalGuid();
        // bob verifies signature and mac
        assertTrue(bobIdManager.verifySignature(aliceId, data, sigBytes));
        assertTrue(bobIdManager.verifyHmac(aliceId, data, macBytes));
        
        // bad signature or mac should fail
        byte[] badSigBytes = sigBytes;
        byte[] badMacBytes = macBytes;
        badSigBytes[10] += 1;
        badMacBytes[10] += 1;
        assertFalse(bobIdManager.verifySignature(aliceId, data, sigBytes));
        assertFalse(bobIdManager.verifyHmac(aliceId, data, macBytes));
    }
    
    public void testSignature() throws Exception {
        // key agreement first
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());        
        aliceIdManager.start();
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(new SecureIdStoreImpl());
        bobIdManager.start();
        Identity request = aliceIdManager.getPublicLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getPublicLocalIdentity();
        aliceIdManager.addIdentity(reply);
        // key agreement done, alice and bob should share a key
                 
        // alice generates signature and mac for bob
        byte[] data = "data".getBytes();
        GUID bobId = bobIdManager.getLocalGuid();
        byte[] sigBytes = aliceIdManager.sign(data);
        byte[] macBytes = aliceIdManager.createHmac(bobId, data);
        // bob knows that the signature and mac are from alice
        GUID aliceId = aliceIdManager.getLocalGuid();
        // bob verifies signature and mac
        assertTrue(bobIdManager.verifySignature(aliceId, data, sigBytes));
        assertTrue(bobIdManager.verifyHmac(aliceId, data, macBytes));
        
        // bad signature or mac should fail
        byte[] badSigBytes = sigBytes;
        byte[] badMacBytes = macBytes;
        badSigBytes[10] += 1;
        badMacBytes[10] += 1;
        assertFalse(bobIdManager.verifySignature(aliceId, data, sigBytes));
        assertFalse(bobIdManager.verifyHmac(aliceId, data, macBytes));
    }
    
    
    
}
