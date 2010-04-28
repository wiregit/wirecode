package org.limewire.activation.serial;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.io.IOUtils;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.CipherProvider.CipherType;
import org.limewire.util.Base32;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;

/**
 * Serializes and Deserializes ActivationModules to and from Disk.
 */
class ActivationSerializerImpl implements ActivationSerializer {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final CipherType CIPHER_TYPE = CipherType.AES;
    
    private static final Log LOG = LogFactory.getLog(ActivationSerializerImpl.class);
    
    private final ActivationSerializerSettings settings;
    private final CipherProvider cipherProvider;
    private final ActivationSettingsController activationSettings;
    
    @Inject
    public ActivationSerializerImpl(ActivationSerializerSettings settings, CipherProvider cipherProvider,
            ActivationSettingsController activationSettings){
        this.settings = settings;
        this.cipherProvider = cipherProvider;
        this.activationSettings = activationSettings;
    }

    @Override
    public String readFromDisk() throws IOException {
        if(!settings.getSaveFile().exists() && !settings.getBackupFile().exists())
            return null;
        
        Throwable exception;
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(settings.getSaveFile())));
            String encyrptedString = (String) in.readObject();
            return decrypt(encyrptedString);
        } catch(Throwable ignored) {
            exception = ignored;
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        // Falls through to here only on error with normal file.
        
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(settings.getBackupFile())));
            String encyrptedString = (String) in.readObject();
            return decrypt(encyrptedString);
        } catch(Throwable ignored) {
            LOG.warn("Error reading backup file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        if(exception instanceof IOException)
            throw (IOException)exception;
        else
            throw (IOException)new IOException().initCause(exception);
    }

    @Override
    public boolean writeToDisk(String jsonString) throws IOException, GeneralSecurityException {
        String encrypted = encrypt(jsonString);
        return FileUtils.writeWithBackupFile(encrypted, settings.getBackupFile(), settings.getSaveFile(), LOG);            
    }
    
    /**
     * Encrypts the given String.
     */
    private String encrypt(String message) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        String key = getEncryptionKey();
        SecretKeySpec keySpec = new SecretKeySpec(new BigInteger(key, 16).toByteArray(), ENCRYPTION_ALGORITHM);

        byte[] encryptedBytes = cipherProvider.encrypt(StringUtils.toUTF8Bytes(message), keySpec, CIPHER_TYPE);
        return Base32.encode(encryptedBytes);
    }
    
    /**
     * Decrypts the given String.
     */
    private String decrypt(String encryptedString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, IOException {
        String key = getEncryptionKey();
        SecretKeySpec keySpec = new SecretKeySpec(new BigInteger(key, 16).toByteArray() , ENCRYPTION_ALGORITHM);

        byte[] decryptedBytes = cipherProvider.decrypt(Base32.decode(encryptedString), keySpec, CIPHER_TYPE);

        return new String(decryptedBytes);
    }
    
    /**
     * Returns the encryption/decryption key.
     */
    private String getEncryptionKey() {
        return activationSettings.getPassKey();
    }
}
