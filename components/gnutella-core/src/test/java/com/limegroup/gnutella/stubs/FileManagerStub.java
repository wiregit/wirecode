package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
public class FileManagerStub extends FileManager {
    public FileDesc get(int i) {
        return new FileDesc(new File("com.limegroup.gnutella.tests.stubs.FileManagerStub.java"), 
                            new HashSet(),
							i);
    }
    public InputStream getInputStream(FileDesc ignored) {
        return new InputStream() {
            public int read() {
                return 'a';
            }
        };
    }
}
