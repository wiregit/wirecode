package com.limegroup.gnutella.encryption;

public interface Decrypter {
    void decrypt(byte[] b,int off,int len);
}
