package org.limewire.security.certificate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;

import com.google.inject.Singleton;

/**
 * Initially loads the key store from the network, and then caches it to disk in
 * the preferences directory.
 */
@Singleton
public class KeyStoreProviderImpl implements KeyStoreProvider {
    private KeyStore keyStore = null;

    private File keyStoreLocation;

    private char[] keyStorePassword;

    private final Log LOG = LogFactory.getLog(KeyStoreProviderImpl.class);

    public KeyStoreProviderImpl() {
        this.keyStoreLocation = CertificateTools.getKeyStoreLocation();
        this.keyStorePassword = CertificateTools.getKeyStorePassword();
    }

    public KeyStoreProviderImpl(File keyStoreLocation, char[] keyStorePassword) {
        this.keyStoreLocation = keyStoreLocation;
        this.keyStorePassword = keyStorePassword;
    }

    public KeyStore getKeyStore() throws IOException {
        if (isValid(keyStore))
            return keyStore;
        // Not cached, try to load into memory from disk...
        try {
            keyStore = getKeyStoreFromDisk();
            return keyStore;
        } catch (IOException ex) {
            // Failed to load from disk, ignore and try to pull from network...
            LOG.debug("IOException trying to load keystore from disk.", ex);
        }

        // Not on disk, try to load from network, load into memory and save to
        // disk.
        OutputStream out = null;
        try {
            keyStore = getKeyStoreFromNetwork();
            keyStoreLocation.getParentFile().mkdirs();
            out = new FileOutputStream(keyStoreLocation);
            keyStore.store(out, keyStorePassword);
            return keyStore;
        } catch (KeyStoreException ex) {
            throw IOUtils.getIOException("KeyStoreException while saving keystore: " ,ex);
        } catch (NoSuchAlgorithmException ex) {
            throw IOUtils.getIOException("NoSuchAlgorithmException while saving keystore: ",ex);
        } catch (CertificateException ex) {
            throw new IOException("CertificateException while saving keystore: " + ex.getMessage());
        } finally {
            if (out != null)
                out.close();
        }
    }

    KeyStore getKeyStoreFromNetwork() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) CertificateTools.getKeyStoreURL()
                .openConnection();
        connection.setRequestMethod("GET");

        KeyStore newKeyStore;
        try {
            newKeyStore = KeyStore.getInstance("jks");
        } catch (KeyStoreException ex) {
            throw new IOException("KeyStoreException instantiating keystore: " + ex.getMessage());
        }
        try {
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try {
                    newKeyStore.load(connection.getInputStream(), keyStorePassword);
                } catch (NoSuchAlgorithmException ex) {
                    throw new IOException("NoSuchAlgorithmException while parsing keystore: "
                            + ex.getMessage());
                } catch (CertificateException ex) {
                    throw new IOException("CertificateException while parsing keystore: "
                            + ex.getMessage());
                }
                return newKeyStore;
            } else {
                throw new IOException("Failed to download new keystore ("
                        + CertificateTools.getKeyStoreURL().toString() + "): "
                        + connection.getResponseCode() + " " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * @return the loaded and initialized key store, or throws an exception
     * @throws IOException if loading fails for any reason (missing file,
     *         certificate issues, etc)
     */
    KeyStore getKeyStoreFromDisk() throws IOException {
        InputStream in = null;
        try {
            KeyStore newKeyStore = KeyStore.getInstance("jks");
            in = new BufferedInputStream(new FileInputStream(keyStoreLocation));
            newKeyStore.load(in, keyStorePassword);
            return newKeyStore;
        } catch (KeyStoreException ex) {
            throw new IOException("KeyStoreException while creating keystore in memory: "
                    + ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("NoSuchAlgorithmException while parsing keystore: "
                    + ex.getMessage());
        } catch (CertificateException ex) {
            throw new IOException("CertificateException while parsing keystore: " + ex.getMessage());
        } finally {
            if (in != null)
                in.close();
        }
    }

    /*
     * Invalidates by dereferencing our in-memory copy as well as deleting the
     * on-disk version.
     */
    public void invalidateKeyStore() {
        keyStore = null;
        keyStoreLocation.delete();
    }

    private boolean isValid(KeyStore ks) {
        if (ks == null)
            return false;
        try {
            if (ks.size() > 0)
                return true;
        } catch (KeyStoreException ex) {
            // keyStore is non-null but not initialized, treat it as null
        }
        return false;
    }

    public boolean isCached() {
        if (isValid(keyStore))
            return true;
        if (keyStoreLocation.exists()) {
            try {
                if (isValid(getKeyStoreFromDisk()))
                    return true;
            } catch (IOException ignored) {
                // File exists but doesn't look to be valid...
            }
        }
        return false;
    }

}
