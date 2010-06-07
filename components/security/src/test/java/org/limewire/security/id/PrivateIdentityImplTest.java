package org.limewire.security.id;

import junit.framework.Test;
import org.limewire.util.BaseTestCase;

public class PrivateIdentityImplTest extends BaseTestCase {
    public PrivateIdentityImplTest(String name){
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PrivateIdentityImplTest.class);
    }

    public void testPrivateIdentityImpl() throws Exception{
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl(new SimpleSecureIdStoreImpl());
        
        // testing toByteArray() and privateIdentity constructed from a byte array
        PrivateIdentity privateIdentity = (PrivateIdentity) aliceIdManager.getLocalIdentity(); 
        byte[] privateIdentityBytes = privateIdentity.toByteArray();
        PrivateIdentity reconstructedPrivateIdentity = new PrivateIdentityImpl(privateIdentityBytes);       
        assertEquals(reconstructedPrivateIdentity.getGuid().bytes().length, 16);
        assertEquals(privateIdentity.getGuid().bytes(), reconstructedPrivateIdentity.getGuid().bytes());
        assertEquals(privateIdentity.getSignature(), reconstructedPrivateIdentity.getSignature());
        assertEquals(privateIdentity.getPublicSignatureKey().getEncoded(), reconstructedPrivateIdentity.getPublicSignatureKey().getEncoded());
        assertEquals(0, privateIdentity.getPublicDiffieHellmanComponent().compareTo(reconstructedPrivateIdentity.getPublicDiffieHellmanComponent()));        
        assertEquals(privateIdentity.getMultiInstallationMark(), reconstructedPrivateIdentity.getMultiInstallationMark()); 
        assertEquals(privateIdentity.getPrivateDiffieHellmanKey().getEncoded(), reconstructedPrivateIdentity.getPrivateDiffieHellmanKey().getEncoded());
        assertEquals(privateIdentity.getPrivateSignatureKey().getEncoded(), reconstructedPrivateIdentity.getPrivateSignatureKey().getEncoded());
    }
}