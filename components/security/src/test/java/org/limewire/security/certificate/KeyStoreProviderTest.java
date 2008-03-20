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
        KeyStoreProviderImpl ksp = new KeyStoreProviderImpl();
        KeyStore keystore = ksp.getKeyStoreFromNetwork();
        validateKeyStore(keystore);
    }

    private void validateKeyStore(KeyStore keyStore) throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("ca.limewire.com");
        assertEquals(
                "33D7A0AE224C0920B3DECCD33C937A10B6F1437FE33CD0688D999B94B95C61FBC2768139320B55F893562D5DB8C090990761DE79CB15025CAF113692C5F32AC3B9F0D4C8C1CAF7BBF2ECF4ED4D39EF29FD17033D3B073BC6F18991F2C2CDFCF12463B88012E8F078917D51F890C621626D4683EBE8878636A4B891C55991CBC207541AADED2B3A8DE8FC4322112AB88D81BED4D9078BA356A27EE6A1A59A587A304A58153B95AE82FD30590A3638065A45509C5ED851EC21F926AF9D050E7E2C08328DB81FCA09D55AD517CB8249BD63B94779B1023299F0601E95C7CE1B86F227FFE94D75E6305EE9CA41096976083301C240958F37CF14491CF0E0942A9D082CD031110F93912CA44338B0111B2A6E3FCE3E82F4A4A0254BCD2AA08ABBB526388D544757588F75D82D261C52BABDDDBDDD531679ABA87482F49F956388E4AE16045D25676E54CBF94A612F093D8D19E6FDADEBE3B91B0043B33666FE3A01AECBD545B8D86697814BD27F2A1E0F22221906FB6C9D69433AFA478428B1A6263E56FB67715E5A6A1511A80E8F7E20B29AF7CF43632B9C9EF0794B2E0450AA42764624F187CC80C68788063F95E383C635FE0462D8ED7A3E42DE3F81196F1D2C149DF55B25C0232050A2D9FC4DC4907ECAE5ABA9CD869AFD4430318FAE033D8434F05D3283B067166AA966CFB7F1728014420CF8726027A25F0AB8D70AE670514D",
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

        KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(location, getUnitKeyStorePassword());
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
            KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(location, getUnitKeyStorePassword());
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
