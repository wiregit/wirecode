package org.limewire.security.id;

import java.security.MessageDigest;
import java.util.Arrays;

import junit.framework.Test;

import org.limewire.core.settings.SecuritySettings;
import org.limewire.io.GUID;
import org.limewire.security.id.SecureIdManager.Identity;
import org.limewire.util.BaseTestCase;

public class SecureIdManagerTest extends BaseTestCase {
    public SecureIdManagerTest(String name){
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SecureIdManagerTest.class);
    }

    public void testIdGeneration() throws Exception{
        // generate id, test if it is secure id, verify sturgeon id mark
        GUID sturgeonID = new GUID("00000000000000000000000000000000");
        SecureIdManager aliceIdManager = new SecureIdManager(sturgeonID, null);
        
        MessageDigest md = MessageDigest.getInstance(SecureIdManager.HASH_ALGO);
        md.update(aliceIdManager.getMyIdentity().getSignatureKey().getEncoded());
        byte[] hash = md.digest();
        assertTrue(GUID.isSecureGuid(hash, aliceIdManager.getMyId()));        
    }
        
    public void testKeyAgreement() throws Exception{
        // the two parties of key agreement: alice and bob
        SecureIdManager aliceIdManager = new SecureIdManager(new GUID(), "aliceRemoteKeys.txt");        
        SecuritySettings.SIGNATURE_PUBLIC_KEY.set("set it to this line to rise an exception, otherwise alice and bob will have same keys");
        SecureIdManager bobIdManager = new SecureIdManager(new GUID(), "bobRemoteKeys.txt");        

        // alice and bob exchange identities
        Identity request = aliceIdManager.getMyIdentity();
        bobIdManager.processIdentity(request);
        Identity reply = bobIdManager.getMyIdentity();
        aliceIdManager.processIdentity(reply);
        assertTrue(Arrays.equals(
                aliceIdManager.getRemotePublicKeyEntry(bobIdManager.getMyId()).sharedKey.getEncoded(), 
                bobIdManager.getRemotePublicKeyEntry(aliceIdManager.getMyId()).sharedKey.getEncoded()
                ));
        }
    
    public void testSignatureAndHmac() throws Exception {
        // key agreement first
        SecureIdManager aliceIdManager = new SecureIdManager(new GUID(), "aliceRemoteKeys.txt");        
        SecuritySettings.SIGNATURE_PUBLIC_KEY.set("set it to this line to rise an exception, otherwise alice and bob will have same keys");
        SecureIdManager bobIdManager = new SecureIdManager(new GUID(), "bobRemoteKeys.txt");        
        Identity request = aliceIdManager.getMyIdentity();
        bobIdManager.processIdentity(request);
        Identity reply = bobIdManager.getMyIdentity();
        aliceIdManager.processIdentity(reply);
        // key agreement done, alice and bob should share a key
                 
        // alice generates signature and mac for bob
        byte[] data = "data".getBytes();
        GUID bobId = bobIdManager.getMyId();
        byte[] sigBytes = aliceIdManager.sign(data);
        byte[] macBytes = aliceIdManager.hamc(bobId, data);
        // bob knows that the signature and mac are from alice
        GUID aliceId = aliceIdManager.getMyId();
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
