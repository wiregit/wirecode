package org.limewire.security.id;

import junit.framework.Test;
import org.limewire.util.BaseTestCase;

public class RemoteIdKeysTest extends BaseTestCase {
    public RemoteIdKeysTest(String name){
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RemoteIdKeysTest.class);
    }

    public void testRemoteIdKeys() throws Exception{
        // key agreement first
        SimpleSecureIdStoreImpl aliceStorage = new SimpleSecureIdStoreImpl();
        SimpleSecureIdStoreImpl bobStorage = new SimpleSecureIdStoreImpl();
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(aliceStorage);        
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl(bobStorage);
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        
        // key agreement done, alice and bob should share a key
        // both alice and bob implicitly called remoteIdKeys.toByteArray() to store the shared key to their storage.
        
        // reconstructing
        byte[] remoteIdKeysBytes = bobStorage.get(aliceIdManager.getLocalGuid());
        RemoteIdKeys bobRemoteIdKeys = new RemoteIdKeys(remoteIdKeysBytes);
        remoteIdKeysBytes = aliceStorage.get(bobIdManager.getLocalGuid());
        RemoteIdKeys aliceRemoteIdKeys = new RemoteIdKeys(remoteIdKeysBytes);
        // compare
        assertEquals(aliceRemoteIdKeys.getId().bytes(), bobIdManager.getLocalGuid().bytes());
        assertEquals(aliceRemoteIdKeys.getSignaturePublicKey().getEncoded(), bobIdManager.getLocalIdentity().getPublicSignatureKey().getEncoded());
        assertEquals(aliceRemoteIdKeys.getOutgoingMacHmacKey().getEncoded(), bobRemoteIdKeys.getIncomingVerificationHmacKey().getEncoded());
        assertEquals(aliceRemoteIdKeys.getIncomingVerificationHmacKey().getEncoded(), bobRemoteIdKeys.getOutgoingMacHmacKey().getEncoded());
        assertEquals(aliceRemoteIdKeys.getOutgoingEncryptionKey().getEncoded(), bobRemoteIdKeys.getIncomingDecryptionKey().getEncoded());
        assertEquals(aliceRemoteIdKeys.getIncomingDecryptionKey().getEncoded(), bobRemoteIdKeys.getOutgoingEncryptionKey().getEncoded());
    }
}