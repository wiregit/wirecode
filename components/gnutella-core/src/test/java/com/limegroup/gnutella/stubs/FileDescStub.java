package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Set;

public class FileDescStub extends FileDesc {
    private static Set set ;
    static {
        set = new HashSet();
        try {
            set.add(URN.createSHA1Urn(
                                  "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
    
    public FileDescStub() {
        super(new File("abc.txt"),set,0);
    }

    public InputStream createInputStream() {
        return new InputStream() {
            public int read() {
                return 'a';
            }
            public int read(byte[] b) {
                for(int i=0; i < b.length; i++)
                    b[i] = (byte)'a';
                return b.length;
            }
        };
    }
    
    public long getSize() {
        return 1126400;
    }
}
