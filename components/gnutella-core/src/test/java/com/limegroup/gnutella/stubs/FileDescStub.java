package com.limegroup.gnutella.stubs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.URN;

@SuppressWarnings("unchecked")
public class FileDescStub extends FileDesc {
    public static final String DEFAULT_URN =
        "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
    public static final URN DEFAULT_SHA1;
    public static final Set DEFAULT_SET;
    public static final int DEFAULT_SIZE = 1126400;
    
    static {
        DEFAULT_SET = new HashSet();
        URN sha1 = null;
        try {
            sha1 = URN.createSHA1Urn(DEFAULT_URN);
        } catch(IOException ioe) {
            ErrorService.error(ioe);
        }
        DEFAULT_SHA1 = sha1;
        DEFAULT_SET.add(DEFAULT_SHA1);
    }
    
    public FileDescStub() {
        this("abc.txt");
    }
    
    public FileDescStub(String name) {
        this(name, DEFAULT_SHA1, 0);
    }
    
    public FileDescStub(String name, URN urn, int index) {
    	super(new File(name), createUrnSet(urn), index);
    	
        FileDescStub.createStubFile(this);
    }

    static void createStubFile(FileDesc fd) {
        File file = fd.getFile();
        if (!file.exists()) {
            try {
                OutputStream out = new BufferedOutputStream(
                        new FileOutputStream(file));
                file.deleteOnExit();
                try {
                    int length = (int) fd.getFileSize();
                    for (int i = 0; i < length; i++) {
                        out.write('a');
                    }
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    private static Set createUrnSet(URN urn) {
        Set s = new HashSet();
        s.add(urn);
        return s;
    }

//    public InputStream createInputStream() {
//        return new InputStream() {
//            int read = 0;
//            int length = (int) getFileSize();
//            public int read() {
//                if (read >= length) {
//                    return -1;
//                }
//                read++;
//                return 'a';
//            }
//            public int read(byte[] b) {
//                if (read >= length) {
//                    return -1;
//                }
//                int start = read;
//                for(int i=0; i < b.length && read < length; i++) {
//                    b[i] = (byte)'a';
//                    read++;
//                }
//                return read - start;
//            }
//        };
//    }

    public long getFileSize() {
        return DEFAULT_SIZE;
    }
}
