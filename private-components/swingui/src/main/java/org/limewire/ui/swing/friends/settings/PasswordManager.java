package org.limewire.ui.swing.friends.settings;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.GeneralSecurityException;
import java.util.prefs.Preferences;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.limewire.security.certificate.CipherProvider;

@Singleton
public class PasswordManager {

    private static final String PREFERENCES_NODE = "/limewire/xmpp/auth";

    private CipherProvider cipherProvider;

    @Inject
    public PasswordManager(CipherProvider cipherProvider) {
        this.cipherProvider = cipherProvider;
    }

    public String loadPassword(String userName) throws XMPPEncryptionException {
        if (userName == null || userName.equals("")) {
            throw new IllegalArgumentException("User Name cannot be null or empty String");
        }

        String encryptedPassword = loadEncryptedPassword(userName);
        Password pwd = new Password(cipherProvider, encryptedPassword, true);

        // test to see if we can decrypt it without error
        try {
            return pwd.decryptPassword();
        } catch (IOException e) {
            throw new XMPPEncryptionException("Error decrypting password", e);
        } catch (GeneralSecurityException e) {
            throw new XMPPEncryptionException("Error decrypting password", e);
        }
    }

    public void storePassword(String userName, String rawPassword) throws XMPPEncryptionException {
        if (rawPassword == null || rawPassword.equals("")) {
            throw new IllegalArgumentException("Password cannot be null or empty String");
        }

        Preferences prefs = getPreferences();
        Password pwd = new Password(cipherProvider, rawPassword, false);

        try {
            prefs.put(userName, pwd.encryptPassword());
        } catch (NoSuchAlgorithmException e) {
            throw new XMPPEncryptionException("Error encrypting password", e);
        } catch (IOException e) {
            throw new XMPPEncryptionException("Error encrypting password", e);
        }
    }

    public void removePassword(String userName) {
        Preferences prefs = getPreferences();
        prefs.remove(userName);
    }

    private String loadEncryptedPassword(String userName) {
        Preferences prefs = getPreferences();
        return prefs.get(userName, "");
    }

    private Preferences getPreferences() {
        return Preferences.userRoot().node(PREFERENCES_NODE);
    }
}
