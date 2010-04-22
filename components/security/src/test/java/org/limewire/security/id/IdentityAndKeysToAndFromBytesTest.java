package org.limewire.security.id;

import junit.framework.Test;
import org.limewire.util.BaseTestCase;

public class IdentityAndKeysToAndFromBytesTest extends BaseTestCase {
    public IdentityAndKeysToAndFromBytesTest(String name){
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(IdentityAndKeysToAndFromBytesTest.class);
    }

    public void testIdentityImpl() throws Exception{
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(null);
        aliceIdManager.start();
        
        // testing toByteArray() and identity constructed from a byte array
        Identity identity = aliceIdManager.getPublicLocalIdentity();
        byte[] identityBytes = identity.toByteArray();
        Identity reconstructedIdentity = new IdentityImpl(identityBytes);       
        assertTrue(reconstructedIdentity.getGuid().bytes().length == 16);
        assertEquals(identity.getGuid().bytes(), reconstructedIdentity.getGuid().bytes());
        assertEquals(identity.getSignature(), reconstructedIdentity.getSignature());
        assertEquals(identity.getPublicSignatureKey().getEncoded(), reconstructedIdentity.getPublicSignatureKey().getEncoded());
        assertEquals(0, identity.getPublicDiffieHellmanComponent().compareTo(reconstructedIdentity.getPublicDiffieHellmanComponent()));        
    }

    public void testPrivateIdentityImpl() throws Exception{
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(null);
        aliceIdManager.start();
        
        // testing toByteArray() and privateIdentity constructed from a byte array
        PrivateIdentity privateIdentity = aliceIdManager.getPrivateLocalIdentity(); 
        byte[] privateIdentityBytes = privateIdentity.toByteArray();
        PrivateIdentity reconstructedPrivateIdentity = new PrivateIdentityImpl(privateIdentityBytes);       
        assertTrue(reconstructedPrivateIdentity.getGuid().bytes().length == 16);
        assertEquals(privateIdentity.getGuid().bytes(), reconstructedPrivateIdentity.getGuid().bytes());
        assertEquals(privateIdentity.getSignature(), reconstructedPrivateIdentity.getSignature());
        assertEquals(privateIdentity.getPublicSignatureKey().getEncoded(), reconstructedPrivateIdentity.getPublicSignatureKey().getEncoded());
        assertEquals(0, privateIdentity.getPublicDiffieHellmanComponent().compareTo(reconstructedPrivateIdentity.getPublicDiffieHellmanComponent()));        
        assertEquals(privateIdentity.getMultiInstallationMark(), reconstructedPrivateIdentity.getMultiInstallationMark()); 
        assertEquals(privateIdentity.getPrivateDiffieHellmanKey().getEncoded(), reconstructedPrivateIdentity.getPrivateDiffieHellmanKey().getEncoded());
        assertEquals(privateIdentity.getPrivateSignatureKey().getEncoded(), reconstructedPrivateIdentity.getPrivateSignatureKey().getEncoded());
    }

    public void testRemoteIdKeys() throws Exception{
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
        // both alice and bob implicitly called remoteIdKeys.toByteArray() to store the shared key to their storage.
        
        // reconstructing
        byte[] remoteIdKeysBytes = bobIdManager.getSecureIdStore().get(aliceIdManager.getLocalGuid());
        RemoteIdKeys bobRemoteIdKeys = new RemoteIdKeys(remoteIdKeysBytes);
        remoteIdKeysBytes = aliceIdManager.getSecureIdStore().get(bobIdManager.getLocalGuid());
        RemoteIdKeys aliceRemoteIdKeys = new RemoteIdKeys(remoteIdKeysBytes);
        // compare
        assertEquals(aliceRemoteIdKeys.getId().bytes(), bobIdManager.getLocalGuid().bytes());
        assertEquals(aliceRemoteIdKeys.getSignaturePublicKey().getEncoded(), bobIdManager.getPublicLocalIdentity().getPublicSignatureKey().getEncoded());
        assertEquals(aliceRemoteIdKeys.getMacKey().getEncoded(), bobRemoteIdKeys.getMacKey().getEncoded());
        assertEquals(aliceRemoteIdKeys.getEncryptionKey().getEncoded(), bobRemoteIdKeys.getEncryptionKey().getEncoded());
    }
}