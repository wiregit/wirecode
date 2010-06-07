package org.limewire.security.certificate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import junit.framework.Test;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.limewire.inject.Providers;
import org.limewire.util.BaseTestCase;

public class KeyStoreProviderTest extends BaseTestCase {
    private char[] getUnitKeyStorePassword(){
        return "".toCharArray();
    }

    public KeyStoreProviderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(KeyStoreProviderTest.class);
    }

    public void testGetKeyStoreFromNetwork() throws IOException, KeyStoreException {
        KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(Providers.of((HttpClient)new DefaultHttpClient()));
        KeyStore keystore = ksp.getKeyStoreFromNetwork();
        validateKeyStore(keystore);
    }

    private void validateKeyStore(KeyStore keyStore) throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("ca.limewire.com");
        assertEquals(
                "C56E57FB94475C48F3F6CED0F3723D401F3D9792803878FCF726DADBF0C7058FC026E046A256AFA27E9BAD6C57B33AA56F426001922FD629C28A3018F0AE607421765056EE29B52312DB7088C8A99575316944D17F1808212F176A5DADB876E7C6E2DBC3BC55ED3D31AE333FE88F9CDB1ED10020A31730CA4EA4DF92CCADC2AF",
                CertificateTools.encodeBytesToString(certificate.getSignature()));
    }

    /**
     * Loads the key store from the network, saves it to disk, then calls
     * getKeyStoreFromDisk.
     * 
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public void testGetKeyStoreFromDisk() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        File location = File.createTempFile("lwunit", "keystore");
        location.deleteOnExit();

        KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(Providers.of((HttpClient)new DefaultHttpClient()));
        ksp.setKeyStoreLocation(location);
        ksp.setKeyStorePassword(getUnitKeyStorePassword());
        KeyStore keystore = ksp.getKeyStoreFromNetwork();
        keystore.store(new FileOutputStream(location), getUnitKeyStorePassword());

        // Saved to disk, see if it loads!
        validateKeyStore(ksp.getKeyStoreFromDisk());
        location.delete();
    }

    public void testIsCachedAndInvalidateKeyStore() throws IOException, KeyStoreException {
        File location = File.createTempFile("lwunit", "keystore");
        location.delete();
        try {
            assertFalse(location.exists());
            KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(Providers.of((HttpClient)new DefaultHttpClient()));
            ksp.setKeyStoreLocation(location);
            ksp.setKeyStorePassword(getUnitKeyStorePassword());
            assertFalse(ksp.isCached());

            // Make sure caching works
            validateKeyStore(ksp.getKeyStore());
            assertTrue(location.exists());
            assertTrue(ksp.isCached());
            // Make sure invalidation works
            ksp.invalidateKeyStore();
            assertFalse(location.exists());
            assertFalse(ksp.isCached());
            // Make sure in-memory caching works
            validateKeyStore(ksp.getKeyStore());
            assertTrue(location.exists());
            assertTrue(ksp.isCached());
            location.delete();
            assertFalse(location.exists());
            assertTrue(ksp.isCached());
        } finally {
            location.delete();
        }
    }

}
