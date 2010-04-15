package org.limewire.security.id;

import java.security.MessageDigest;
import java.util.Arrays;

import junit.framework.Test;

import org.limewire.core.settings.SecuritySettings;
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
        md.update(aliceIdManager.getLocalIdentity().getSignatureKey().getEncoded());
        byte[] hash = md.digest();
        assertTrue(GUID.isSecureGuid(hash, aliceIdManager.getLocalGuid(), SecureIdManager.TAGGING));        
    }
        
    public void testKeyAgreement() throws Exception{
        // the two parties of key agreement: alice and bob
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl("aliceRemoteKeys.txt");
        aliceIdManager.start();
        SecuritySettings.SIGNATURE_PUBLIC_KEY.set("set it to this line to rise an exception, otherwise alice and bob will have same keys");
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl("bobRemoteKeys.txt");        
        bobIdManager.start();
        // alice and bob exchange identities
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
        aliceIdManager.addIdentity(reply);
        assertTrue(Arrays.equals(
                aliceIdManager.getRemoteIdKeys(bobIdManager.getLocalGuid()).getEncryptionKey().getEncoded(), 
                bobIdManager.getRemoteIdKeys(aliceIdManager.getLocalGuid()).getEncryptionKey().getEncoded()
                ));
        }
    
    public void testSignatureAndHmac() throws Exception {
        // key agreement first
        SecureIdManagerImpl aliceIdManager = new SecureIdManagerImpl("aliceRemoteKeys.txt");        
        aliceIdManager.start();
        SecuritySettings.SIGNATURE_PUBLIC_KEY.set("set it to this line to rise an exception, otherwise alice and bob will have same keys");
        SecureIdManagerImpl bobIdManager = new SecureIdManagerImpl("bobRemoteKeys.txt");
        bobIdManager.start();
        Identity request = aliceIdManager.getLocalIdentity();
        bobIdManager.addIdentity(request);
        Identity reply = bobIdManager.getLocalIdentity();
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
