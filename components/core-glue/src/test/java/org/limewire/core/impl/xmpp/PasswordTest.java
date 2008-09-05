package org.limewire.core.impl.xmpp;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.AbstractModule;
import junit.framework.Test;
import org.limewire.security.LimeWireSecurityModule;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.util.BaseTestCase;
import org.limewire.http.httpclient.LimeHttpClient;
import org.limewire.http.httpclient.SimpleLimeHttpClient;

/**
 * Unit tests for {@link Password}
 */
public class PasswordTest extends BaseTestCase {

    private CipherProvider cipherProvider;

    public PasswordTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PasswordTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = Guice.createInjector(new LimeWireSecurityModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(LimeHttpClient.class).to(SimpleLimeHttpClient.class);
            }
        });
        cipherProvider = injector.getInstance(CipherProvider.class);
    }

    /**
     * Test encrypting and decrypting 
     */
    public void testPasswordEncryptionDecryptionPositiveTest() throws Exception {
        String originalPasswd = "thisisasampletestpasswordlongerthanusual";
        Password passwd = new Password(cipherProvider, originalPasswd, false);
        String encryptedPasswd = passwd.encryptPassword();

        Password passwd2 = new Password(cipherProvider, encryptedPasswd, true);
        String decryptedPasswd = passwd2.decryptPassword();

        assertEquals(originalPasswd, decryptedPasswd);
    }

    /**
     * Test encrypting, modifying the encrypted password, then decrypting
     *
     * Should fail to decrypt.
     */
    public void testPasswordDecryptionWithInvalidEncryptedPassword() throws Exception {
        String shouldFailToDecryptString = "nonsensicalstring";
        Password passwd = new Password(cipherProvider, shouldFailToDecryptString, true);

        try {
            passwd.decryptPassword();
            fail("Should have encountered error decrypting password!");
        } catch (XmppEncryptionException e) {
            // received expected error    
        }
    }

    

}
