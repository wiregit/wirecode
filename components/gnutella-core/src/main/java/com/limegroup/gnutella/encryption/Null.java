package com.limegroup.gnutella.encryption;

public class Null {

	public static class Encrypter implements 
                                  com.limegroup.gnutella.encryption.Encrypter {
        public byte[] encrypt(byte[] data) {return data;}
	}
    
    public static class Decrypter implements  
                              com.limegroup.gnutella.encryption.Decrypter {
        public void decrypt(byte[] b,int off,int len) {}
    }
}
