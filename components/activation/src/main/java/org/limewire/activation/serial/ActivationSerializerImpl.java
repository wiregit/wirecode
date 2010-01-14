package org.limewire.activation.serial;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.CipherProvider.CipherType;
import org.limewire.setting.ActivationSettings;
import org.limewire.util.Base32;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;

/**
 * Serializes and Deserializes ActivationModules to and from Disk.
 */
public class ActivationSerializerImpl implements ActivationSerializer {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    
    private static final Log LOG = LogFactory.getLog(ActivationSerializerImpl.class);
    
    private final ActivationSerializerSettings settings;
    private final CipherProvider cipherProvider;
    
    @Inject
    public ActivationSerializerImpl(ActivationSerializerSettings settings, CipherProvider cipherProvider){
        this.settings = settings;
        this.cipherProvider = cipherProvider;
    }

    @Override
    public synchronized String readFromDisk() throws IOException {
        if(!settings.getSaveFile().exists() && !settings.getSaveFile().exists())
            return null;
        
        Throwable exception;
        ObjectInputStream in = null;
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(settings.getSaveFile())));
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
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(settings.getBackupFile())));
            String line = (String) in.readObject();
            return decrypt(line);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        if(exception instanceof IOException)
            throw (IOException)exception;
        else
            throw (IOException)new IOException().initCause(exception);
    }

    @Override
    public synchronized boolean writeToDisk(String jsonString) throws Exception {
        String encrypted = encrypt(jsonString);
        return FileUtils.writeWithBackupFile(encrypted, settings.getBackupFile(), settings.getSaveFile(), LOG);            
    }
    
    private String encrypt(String message) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        String key = getEncryptionKey();
        SecretKeySpec keySpec = new SecretKeySpec(new BigInteger(key, 16).toByteArray(), ENCRYPTION_ALGORITHM);

        byte[] bytes2 = cipherProvider.encrypt(message.getBytes("UTF-8"), keySpec, CipherType.AES);
        return Base32.encode(bytes2);
    }
    
    private String decrypt(String encryptedString) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, IOException {
        String key = getEncryptionKey();
        SecretKeySpec keySpec = new SecretKeySpec(new BigInteger(key, 16).toByteArray() , ENCRYPTION_ALGORITHM);

        byte[] answer = cipherProvider.decrypt(Base32.decode(encryptedString), keySpec, CipherType.AES);

        return new String(answer);
    }
    
    private synchronized String getEncryptionKey() {
        String key = ActivationSettings.PASS_KEY.get();
        if(key == null || ActivationSettings.PASS_KEY.isDefault()) {
            key = generateEncryptionKey();
            ActivationSettings.PASS_KEY.set(key);
        }
        return key;
    }
    
    private String generateEncryptionKey() {
        KeyGenerator kgen;
        try {
            kgen = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            kgen.init(128);
          
            SecretKey key = kgen.generateKey();
            byte[] raw = key.getEncoded();
            return asHex(raw);
        } catch(NoSuchAlgorithmException e) {
            // use the default key if it can't be generated
            return ActivationSettings.PASS_KEY.get();
        }
    }
    
    public static String asHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]).toUpperCase());
        }
        return (buf.toString());
    }


    /**
     *  method to convert a byte to a hex string.
     *
     * @param  data  the byte to convert
     * @return String the converted byte
     */
    public static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
    }
    
    /**
     *  Convenience method to convert an int to a hex char.
     *
     * @param  i  the int to convert
     * @return char the converted char
     */
    public static char toHexChar(int i) {
        if ((0 <= i) && (i <= 9)) {
            return (char) ('0' + i);
        } else {
            return (char) ('a' + (i - 10));
        }
    }

//
//    
//    public static void main(String args[]) {
//        ActivationSerializerSettings settings = new ActivationSerializerSettingsImpl();
//        CipherProvider cipher = new CipherProviderImpl();
//        
//        ActivationSerializerImpl activation = new ActivationSerializerImpl(settings, cipher);
//        String testMessage = "this is my message";
//        String testKey = "3A931AF193AC44F66540CFFC57C3978D";
//
//        
//        try {
//            activation.writeToDisk(testMessage);
//            String output = activation.readFromDisk();
//            
//            System.out.println("output "+ output);
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
////        KeyGenerator kgen;
//        try {
////            kgen = KeyGenerator.getInstance("AES");
////            kgen.init(128); // 192 and 256 bits may not be available
////            
////            SecretKey key = kgen.generateKey();
////            byte[] raw = key.getEncoded();
//            
//            SecretKeySpec keySpec = new SecretKeySpec(new BigInteger(testKey, 16).toByteArray() , "AES");
//////            System.out.println(new BigInteger(raw, 16).toByteArray());
////            System.out.println(asHex(raw));
//////            
////                CipherProviderImpl cipher = new CipherProviderImpl();
////                SecretKeySpec skeySpec = new SecretKeySpec(keys.getBytes(), "AES");
////                SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
////                sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
////                sun.misc.BASE64Decoder decoder = new sun.misc.BASE64Decoder();
//                byte[] bytes = cipher.encrypt(testMessage.getBytes("UTF-8"), keySpec, CipherType.AES);
//                String encryptedString = Base32.encode(bytes);//asHex(bytes);
//                
////                new BigInteger(encryptedString, 16).toByteArray()
//                byte[] answer = cipher.decrypt(Base32.decode(encryptedString), keySpec, CipherType.AES);
////                String encryptedMessage = encrypt(testMessage, raw);
//                System.out.println("the decrypted message is: " + new String(answer));//decrypt(encryptedMessage, new String(raw)));
//            } catch (UnsupportedEncodingException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
////        
////        } catch (NoSuchAlgorithmException e) {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////        }
////        } catch (NoSuchAlgorithmException e) {
////                // TODO Auto-generated catch block
////                e.printStackTrace();
////            }
//
//    }
}
