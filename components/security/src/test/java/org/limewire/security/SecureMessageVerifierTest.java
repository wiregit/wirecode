package org.limewire.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


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
        SecureMessageVerifier vf = new SecureMessageVerifier("","") {
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
        SimpleVerifier() {
            super("GCBADOBQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7" +
                    "VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5" +
                    "RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O" +
                    "5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV" +
                    "37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2B" +
                    "BOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOE" +
                    "EBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7Y" +
                    "L7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76" +
                    "Z5ESUA4BQUAAFAMBACDW4TNFXK772ZQN752VPKQSFXJWC6PPSIVTHKDNLRUIQ7UF" +
                    "4J2NF6J2HC5LVC4FO4HYLWEWSB3DN767RXILP37KI5EDHMFAU6HIYVQTPM72WC7FW" +
                    "SAES5K2KONXCW65VSREAPY7BF24MX72EEVCZHQOCWHW44N4RG5NPH2J4EELDPXMNR" +
                    "WNYU22LLSAMBUBKW3KU4QCQXG7NNY", "Simple Verifier");
        }
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
