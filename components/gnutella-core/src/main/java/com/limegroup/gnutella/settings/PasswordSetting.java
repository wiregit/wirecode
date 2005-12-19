package com.limegroup.gnutella.settings;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import com.limegroup.gnutella.ErrorService;

/**
 * Class for a password setting.
 */
public final class PasswordSetting extends Setting {

    /* 
     * Q: Why MD5 and not SHA1 as default?
     * A: MD5 is used in the Digest HTTP Authentication method
     *    and we can recycle the password for it if necessary
     */
    
    /** MD5 algorithm */
    public static final String MD5 = "MD5";
    
    /** Array of supported algorithms */
    private static final String[] ALGORITHMS = { MD5 };
    
    /** Separates the algorithm name and the encrypted password */
    private static final String SEPERATOR = "/";
    
    /** The hex numbers */
    private static final char[] HEX = { 
        '0', '1', '2', '3', '4', '5', 
        '6', '7', '8', '9', 'A', 'B', 
        'C', 'D', 'E', 'F' 
    };
    
    /** The encryption algorithm */
    private String algorithm;
    
    private String value;

    /**
     * Creates a new <tt>PasswordSetting</tt> instance with the specified key
     * and defualt value.
     * 
     * @param key the constant key to use for the setting
     * @param defaultStr the default value to use for the setting
     */
    PasswordSetting(Properties defaultProps, Properties props, String algorithm, String key,
            String defaultStr) {
        this(defaultProps, props, algorithm, key, defaultStr, null);
    }

    PasswordSetting(Properties defaultProps, Properties props, String algorithm, String key,
            String defaultStr, String simppKey) {
        super(defaultProps, props, key, 
                (isEncrypted(defaultStr) ? defaultStr : encrypt(algorithm, defaultStr)), 
                simppKey);
        postInitWithAlgorithm(algorithm);
        setPrivate(true);
    }

    /**
     * Returns the encryption algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Returns true if the passed password equals the
     * current password
     */
    public boolean equals(String password) {
        if (password == null) {
            return false;
        }
        
        if (!isEncrypted(password)) {
            password = encrypt(algorithm, password);
        }
        
        if (!password.startsWith(algorithm + SEPERATOR)) {
            throw new IllegalArgumentException("Algorithm mismatch");
        }
        
        return value.equalsIgnoreCase(password);
    }
    
    /**
     * Accessor for the value of this setting.
     * 
     * @return the value of this setting
     */
    public String getValue() {
        return value;
    }

    /**
     * Mutator for this setting.
     * 
     * @param str the <tt>String</tt> to store
     */
    public void setValue(String str) {
        super.setValue(str);
    }

    /**
     * Load value from property string value
     * 
     * @param sValue property string value
     */
    protected void loadValue(String sValue) {
        if (algorithm != null && !isEncrypted(sValue)) {
            setValue(encrypt(algorithm, sValue));
            return;
        }
        value = sValue;
    }
    
    /**
     * Sets the algorithm and encrypts the password if
     * necessary
     */
    private void postInitWithAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        
        if (algorithm != null && !isEncrypted(value)) {
            setValue(encrypt(algorithm, value));
        }
    }
    
    /**
     * Returns true if password is encrypted (i.e. starts with
     * a known algorithm prefix)
     */
    private static boolean isEncrypted(String password) {
        for(int i = 0; i < ALGORITHMS.length; i++) {
            if (password.startsWith(ALGORITHMS[i] + SEPERATOR)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Encrypts the password and returns it as hex string
     */
    private static String encrypt(String algorithm, String password) {
        return encrypt(algorithm, "UTF-8", password);
    }
    
    /**
     * Encrypts the password and returns it as hex string
     */
    private static String encrypt(String algorithm, String encoding, String password) {
        if (password == null)
            return null;
        
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(password.getBytes(encoding));
            return algorithm + SEPERATOR + toHexString(digest);
        } catch (UnsupportedEncodingException err) {
            ErrorService.error(err);
            return null;
        } catch (NoSuchAlgorithmException err) {
            ErrorService.error(err);
            return null;
        }
    }
    
    /**
     * Encodes and returns b as hex string
     */
    private static String toHexString(byte[] b) {
        StringBuffer buffer = new StringBuffer(b.length * 2);
        for(int i = 0; i < b.length; i++) {
            buffer.append(HEX[((b[i] >> 4) & 0xF)]).append(HEX[b[i] & 0xF]);
        }
        return buffer.toString();
    }
}
