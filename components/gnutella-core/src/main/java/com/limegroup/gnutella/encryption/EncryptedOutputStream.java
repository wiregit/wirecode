package com.limegroup.gnutella.encryption;

import java.io.*;

/**
 * Output stream which delegates to a regular output stream - after encrypting 
 * data using the supplier encrypter.
 */
public class EncryptedOutputStream extends OutputStream {

    //implementation
    private Encrypter _encrypter;
    private OutputStream _delegate;
    
	public EncryptedOutputStream(Encrypter encrypter, OutputStream delegate) {
        _encrypter = encrypter;
        _delegate = delegate;
	}
    
    /**
     * Write out a single encrypted byte
     */
    public void write(int b) throws IOException {
        byte data = (byte) b;
        byte[] encryptedData = _encrypter.encrypt(new byte[] {data});
        for(int i = 0; i < encryptedData.length; i++) {
            _delegate.write(encryptedData[i]);
        }
    }

    public void write(byte[] b,int off,int len) throws IOException {
        byte[] data = new byte[len];
        System.arraycopy(b,off,data,0,len);
        byte[] encryptedData = _encrypter.encrypt(data);
        _delegate.write(encryptedData);
    }
    public void flush() throws IOException {
        _delegate.flush();
    }
}
