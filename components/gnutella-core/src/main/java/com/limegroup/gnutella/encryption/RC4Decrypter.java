package com.limegroup.gnutella.encryption;

public class RC4Decrypter implements Decrypter {

    private ByteSequence _byteSequence;

    public RC4Decrypter(byte[] key) {
        _byteSequence = new ByteSequence(key);
    }
    
    public void decrypt(byte[] b,int off,int len) {
        for(int i = off; i < off+len; i++) {
            b[i] = (byte) (b[i] ^ _byteSequence.next());
        }
    }
}
