package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.ErrorService;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Set;

public class FileDescStub extends FileDesc {
    public static final String urn = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
    public static final Set set;
    public static final int size = 1126400;
    static {
        set = new HashSet();
        try {
            set.add(URN.createSHA1Urn(urn));
        } catch(IOException ioe) {
            ErrorService.error(ioe);
        }
    }
    
    public FileDescStub() {
        this("abc.txt");
    }
    
    public FileDescStub(String name) {
        super(new File(name), set, 0);
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
        return size;
    }
}
