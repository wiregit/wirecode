package com.limegroup.gnutella.encryption;

import java.io.*;

/**
 * decrypts encypted input stream. Gets the data from the delegate 
 * InputStream and asks the supplied decrypter to decrypt
 */

public class DecryptedInputStream extends InputStream {

    private Decrypter _decrypter;
    private InputStream _delegate;    

	public DecryptedInputStream(Decrypter decrypter, InputStream delegate) {
        _decrypter=decrypter;
        _delegate=delegate;
	}

    
    public int read() throws IOException {
        int value = _delegate.read();
        if(value==-1) 
            return -1;
        byte[] data = new byte[] {(byte)value};
        _decrypter.decrypt(data,0,1);
        return asIntFrom0To255(data[0]);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = _delegate.read(b,off,len);
        if(bytesRead == -1) 
            return -1;
        _decrypter.decrypt(b,off,bytesRead);
        return bytesRead;
    }
    
    public static int asIntFrom0To255(byte b) {
        return b & 0x000000FF;
    }

}
