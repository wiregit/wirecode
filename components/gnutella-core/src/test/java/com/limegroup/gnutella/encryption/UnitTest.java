package com.limegroup.gnutella.encryption;

import java.math.*;
import java.util.*;
import com.limegroup.gnutella.*;
import java.io.*;

/**
 * Unit Tests for all classes in the package
 */
public class UnitTest {
    public static void main(String[] args) {
        DiffieHellmanKeyNegotiator negotiator1 =
                                  new DiffieHellmanKeyNegotiator(128);
        DiffieHellmanKeyNegotiator negotiator2 = 
                                   new DiffieHellmanKeyNegotiator(128);

        byte[] neg1Key = negotiator1.keyForOtherSide();
        byte[] neg2Key = negotiator2.keyForOtherSide();

        Assert.that(Arrays.equals(negotiator1.generateSymmetricKey(neg2Key),
                                  negotiator2.generateSymmetricKey(neg1Key)));
        
        testN();
        testEnpryptedOutputStream();
         testWorksWithCorrectPassword();
         testDoesNotWorkWithIncorrectPassword();
         testDecryptedInputStream();
         testBulkRead();
         testNegativeOne();
    }


    public static void testN() {
        int certainty = 1000000000;//prob = (1 - (1/(2^1000000000))) =~ 1
        Assert.that(DiffieHellmanKeyNegotiator.N.isProbablePrime(certainty));
        Assert.that(DiffieHellmanKeyNegotiator.N.subtract(new BigInteger("1")).divide(new BigInteger("2")).isProbablePrime(certainty));
    }


    public void testEnpryptedOutputStream() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encrypter addOneToEachByteEncrypter = new AddOneToEachByteEncrypter();
        OutputStream encryptingOutputStream = 
                     new EncryptedOutputStream(addOneToEachByteEncrypter,out);
        encryptingOutputStream.write(1);
        Assert.that(Arrays.equals(out.toByteArray(), new byte[] {2}));

        addOneToEachByteEncrypter=new AddOneToEachByteEncrypter();
         encryptingOutputStream = 
                     new EncryptedOutputStream(addOneToEachByteEncrypter,out);
        encryptingOutputStream.write(new byte[] {1,2,3,4});
        Assert.that(Arrays.equals(out.toByteArray(), new byte[] {2,3,4,5}));
    }
    
    
    class AddOneToEachByteEncrypter implements Encrypter {
        public byte[] encrypt(byte[] data) {
            byte[] result = new byte[data.length];
            for(int i = 0; i < data.length; i++) {
                result[i] = (byte)(data[i]+1);
            }
            return result;
        }
    }
    ///////
    public void testWorksWithCorrectPassword() throws Exception {
        Encrypter encrypter = new RC4Encrypter("password".getBytes());
        Decrypter decrypter = new RC4Decrypter("password".getBytes());
        byte[] data = new byte[] {1,2,3,4};
        byte[] result = encrypter.encrypt(data);
        decrypter.decrypt(result,0,4);
        Assert.that(Arrays.equals(result,data));
    }

    public void testDoesNotWorkWithIncorrectPassword() throws Exception {
        Encrypter encrypter = new RC4Encrypter("password".getBytes());
        Decrypter decrypter = new RC4Decrypter("badpassword".getBytes());
        byte[] data = new byte[] {1,2,3,4};
        byte[] result = encrypter.encrypt(data);
        decrypter.decrypt(result,0,4);
        Assert.that(!Arrays.equals(result,data));
    }

    ////
    
	//validation
    
    public void testDecryptedInputStream() throws Exception {
        byte[] data = new byte[] {1,2,3,4};
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        Decrypter addOneToEachByteDecrypter = new AddOneToEachByteDecrypter();
        InputStream decryptingInputStream = 
                      new DecryptedInputStream(addOneToEachByteDecrypter,in);
        for(int i = 0; i < data.length; i++) {
            Assert.that(decryptingInputStream.read()==data[i]+1);
        }
    }

    public void testBulkRead() throws Exception {
           byte[] data = new byte[] {1,2,3,4};
           ByteArrayInputStream in = new ByteArrayInputStream(data);
           Decrypter addOneToEachByteDecrypter=new AddOneToEachByteDecrypter();
           InputStream decryptingInputStream = 
                         new DecryptedInputStream(addOneToEachByteDecrypter,in);
           byte[] read = new byte[data.length];
           decryptingInputStream.read(read);
           for(int i = 0; i < data.length; i++) {
               Assert.that(read[i]==data[i]+1);
           }
    }
     
    public void testNegativeOne() throws Exception {
           byte[] data = new byte[] {-1};
           ByteArrayInputStream in = new ByteArrayInputStream(data);
           InputStream decryptingInputStream = 
                        new DecryptedInputStream(new Null.Decrypter(),in);
           for(int i = 0; i < data.length; i++) {
               Assert.that(decryptingInputStream.read()==255);
           }
       }
    
    static class AddOneToEachByteDecrypter implements Decrypter {
        public byte[] decrypt(byte[] data) {
            byte[] result = new byte[data.length];
            for(int i = 0; i < data.length; i++) {
                result[i] = (byte)(data[i]+1);
            }
            return result;
        }
        public void decrypt(byte[] b,int off,int len) {
            for(int i = off; i < off+len; i++) {
                b[i] = (byte)(b[i]+1);
            }
        }
    }

}
