package com.limegroup.gnutella.encryption;

public class RC4Encrypter implements Encrypter {
    
    private ByteSequence _byteSequence;
   
    public RC4Encrypter(byte[] key) {
        _byteSequence = new ByteSequence(key);
    }

    public byte[] encrypt(byte[] data) {
        byte[] encryptedData = new byte[data.length];
        for(int i = 0; i < data.length; i++) {
            encryptedData[i] = (byte) (data[i] ^ _byteSequence.next());
        }
        return encryptedData;
    }
}
