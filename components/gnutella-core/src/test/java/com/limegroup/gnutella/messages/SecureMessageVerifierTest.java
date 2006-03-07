package com.limegroup.gnutella.messages;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Random;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;

public class SecureMessageVerifierTest extends BaseTestCase {

    private static PublicKey PUBLIC_KEY;

    private static PrivateKey PRIVATE_KEY;

    public SecureMessageVerifierTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SecureMessageVerifierTest.class);
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }

    /** gen some fake private/public keys so we can create messages easily. */
    public static void globalSetUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(512, random);
        KeyPair pair = keyGen.generateKeyPair();
        PUBLIC_KEY = pair.getPublic();
        PRIVATE_KEY = pair.getPrivate();
    }

    /** Makes sure all stuff we need to work works. */
    public void testSecureUpdateMessage() throws Exception {
        StaticMessages.initialize();
        QueryReply reply = StaticMessages.getUpdateReply();
        assertTrue(reply.hasSecureData());

        SecureMessageVerifier verifier = new SecureMessageVerifier();
        StubSecureMessageCallback callback = new StubSecureMessageCallback();
        verifier.verify(reply, callback);
        callback.waitForReply();
        assertTrue(callback.getPassed());
        assertSame(reply, callback.getSecureMessage());
        assertEquals(SecureMessage.SECURE, reply.getSecureStatus());
    }

    /** Tests a secure message we create ourselves. */
    public void testSecuring() throws Exception {
        SecureMessage m1 = new StubSecureMessage();
        SecureMessageVerifier vf = new SimpleVerifier();
        StubSecureMessageCallback cb = new StubSecureMessageCallback();
        vf.verify(m1, cb);
        cb.waitForReply();
        assertEquals(m1, cb.getSecureMessage());
        assertEquals(true, cb.getPassed());
        assertEquals(SecureMessage.SECURE, m1.getSecureStatus());
    }
    
    /** Tests a bad message doesn't secure. */
    public void testFails() throws Exception {
        SecureMessage m1 = new StubSecureMessage() {
            public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
                super.updateSignatureWithSecuredBytes(signature);
                signature.update(new byte[100]);
            }
        };
        SecureMessageVerifier vf = new SimpleVerifier();
        StubSecureMessageCallback cb = new StubSecureMessageCallback();
        vf.verify(m1, cb);
        cb.waitForReply();
        assertEquals(m1, cb.getSecureMessage());
        assertEquals(false, cb.getPassed());
        assertEquals(SecureMessage.FAILED, m1.getSecureStatus());
    }
    
    /** Tests a message with no signature. */
    public void testNoSignature() throws Exception {
        SecureMessage m1 = new SecureMessageAdapter();
        SecureMessageVerifier vf = new SimpleVerifier();
        StubSecureMessageCallback cb = new StubSecureMessageCallback();
        vf.verify(m1, cb);
        cb.waitForReply();
        assertEquals(m1, cb.getSecureMessage());
        assertEquals(false, cb.getPassed());
        assertEquals(SecureMessage.INSECURE, m1.getSecureStatus());       
    }
    
    /** Tests attempting to secure with no public key. */
    public void testNoPublicKey() throws Exception {
        SecureMessage m1 = new StubSecureMessage();
        SecureMessageVerifier vf = new SecureMessageVerifier() {
            protected PublicKey createPublicKey() {
                return null;
            }
        };
        StubSecureMessageCallback cb = new StubSecureMessageCallback();
        vf.verify(m1, cb);
        cb.waitForReply();
        assertEquals(m1, cb.getSecureMessage());
        assertEquals(false, cb.getPassed());
        assertEquals(SecureMessage.INSECURE, m1.getSecureStatus());              
    }
    
    /** Tests attempting to secure with a different signature. */
    public void testWrongSignature() throws Exception {
        SecureMessage m1 = new StubSecureMessage() {
            public byte[] getSecureSignature() {
                byte[] sig = super.getSecureSignature();
                sig[0]++;
                return sig;
            }
        };
        SecureMessageVerifier vf = new SimpleVerifier();
        StubSecureMessageCallback cb = new StubSecureMessageCallback();
        vf.verify(m1, cb);
        cb.waitForReply();
        assertEquals(m1, cb.getSecureMessage());
        assertEquals(false, cb.getPassed());
        assertEquals(SecureMessage.FAILED, m1.getSecureStatus());
    }    

    /** Verifier that'll use our fake public key. */
    private static class SimpleVerifier extends SecureMessageVerifier {
        protected PublicKey createPublicKey() {
            return PUBLIC_KEY;
        }
    }

    /** Simple SecureMessage. */
    private static class StubSecureMessage extends SecureMessageAdapter {
        private byte[] data;

        private static Random random = new Random();

        private byte[] signature;

        StubSecureMessage() throws Exception {
            data = new byte[1024];
            random.nextBytes(data);
            Signature sig = Signature.getInstance("SHA1withDSA");
            sig.initSign(PRIVATE_KEY);
            sig.update(data);
            signature = sig.sign();
        }

        public byte[] getSecureSignature() {
            return signature;
        }

        public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
            signature.update(data);
        }
    }

    /** An Adapter so we can do different stuff easily if we want. */
    private static class SecureMessageAdapter implements SecureMessage {
        private int secureStatus = -10;

        public byte[] getSecureSignature() {
            return null;
        }

        public int getSecureStatus() {
            return secureStatus;
        }

        public void setSecureStatus(int secureStatus) {
            this.secureStatus = secureStatus;
        }

        public void updateSignatureWithSecuredBytes(Signature signature) throws SignatureException {
        }
    }
}
