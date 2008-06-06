package org.limewire.activation;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.KeyStoreException;
import java.util.Date;

import junit.framework.Test;

import org.limewire.activation.exception.ActivationException;
import org.limewire.collection.Tuple;
import org.limewire.security.certificate.CertificateProvider;
import org.limewire.security.certificate.CertificateProviderImpl;
import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProviderImpl;
import org.limewire.security.certificate.KeyStoreProvider;
import org.limewire.util.BaseTestCase;

public class ActivationKeyParserImplTest extends BaseTestCase {
    public ActivationKeyParserImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ActivationKeyParserImplTest.class);
    }

    public void testParseBodyAndSignature() throws ActivationException {
        String toParse = ActivationConstants.ACTIVATION_KEY_BEGIN + "\nbody\n"
                + ActivationConstants.ACTIVATION_SIGN_BEGIN + "\nsignature\n"
                + ActivationConstants.ACTIVATION_SIGN_END;
        Tuple<String, String> parsed = new ActivationKeyParserImpl(null, null)
                .parseBodyAndSignature(toParse);
        assertEquals("body\n", parsed.getFirst());
        assertEquals("signature\n", parsed.getSecond());
    }

    public void testParseBodyAndSignatureWithExtraneousInfo() throws ActivationException {
        String toParse = "useless\nuseless\ninfo\n" + ActivationConstants.ACTIVATION_KEY_BEGIN
                + "\nbody\n" + ActivationConstants.ACTIVATION_SIGN_BEGIN + "\nsignature\n"
                + ActivationConstants.ACTIVATION_SIGN_END + "\nuseless!";
        Tuple<String, String> parsed = new ActivationKeyParserImpl(null, null)
                .parseBodyAndSignature(toParse);
        assertEquals("body\n", parsed.getFirst());
        assertEquals("signature\n", parsed.getSecond());
    }

    public void testParseBodyAndGGEP() throws ActivationException {
        String toParse = "foo=bar\nbar=jar\n\nI'm a signature,\nyes I am.";
        Tuple<String, String> parsed = new ActivationKeyParserImpl(null, null)
                .parseBodyAndGGEP(toParse);
        assertEquals("foo=bar\nbar=jar\n", parsed.getFirst());
        assertEquals("I'm a signature,yes I am.", parsed.getSecond());
    }

    public void testGenerate() throws NoSuchAlgorithmException, ActivationException {
        ActivationKeyParserImpl akp = new ActivationKeyParserImpl(new CipherProviderImpl(), null);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();
        ActivationKey activationKey = new ActivationKey();
        activationKey.setValidFrom(new Date());

        akp.generate("person=Beano Smith", activationKey, keyPair.getPrivate());
        // We're encoded... See if it parses...
    }
    
    public void testGenerateAndParse() throws NoSuchAlgorithmException, ActivationException, KeyStoreException, 
      IOException, CertificateException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();

        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null);
        keyStore.setCertificateEntry(
          ActivationConstants.ACTIVATION_CERTIFICATE_ALIAS, new MockCertificate(keyPair.getPublic()));
        
        MockKeyStoreProvider keyStoreProvider       = new MockKeyStoreProvider(keyStore);
        MockCertificateVerifier certificateVerifier = new MockCertificateVerifier();
        
        CertificateProvider certificateProvider = new CertificateProviderImpl(keyStoreProvider, certificateVerifier);
        
        
        ActivationKeyParserImpl akp = new ActivationKeyParserImpl(new CipherProviderImpl(), certificateProvider);
        ActivationKey activationKey = new ActivationKey();
        activationKey.setValidFrom(new Date());

        String encodedKey = akp.generate("person=Beano Smith", activationKey, keyPair.getPrivate());
        // We're encoded... See if it parses...
        ActivationKey parseResult = akp.parse(encodedKey);
        // Damn I'm good!
    }
    
    /** Fake certificate, just holds the public key. */
    private class MockCertificate extends Certificate {
        private PublicKey publicKey;

        MockCertificate(PublicKey publicKey) {
            super(publicKey.getAlgorithm());
            this.publicKey = publicKey;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public PublicKey getPublicKey() {
            return publicKey;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException, SignatureException {
            // TODO Auto-generated method stub
        }

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException,
                NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
                SignatureException {
            // TODO Auto-generated method stub
        }
    }

    /** Always returns the passed-in keystore. */
    private class MockKeyStoreProvider implements KeyStoreProvider {
        private KeyStore keystore;

        MockKeyStoreProvider(KeyStore keystore) {
            this.keystore = keystore;
        }

        public KeyStore getKeyStore() throws IOException {
            return keystore;
        }

        public void invalidateKeyStore() {
        }

        public boolean isCached() {
            return false;
        }
    }

    /** Always validates certificates. */
    private class MockCertificateVerifier implements CertificateVerifier {

        public boolean isValid(java.security.cert.Certificate certificate) {
            return true;
        }

    }
    
}
