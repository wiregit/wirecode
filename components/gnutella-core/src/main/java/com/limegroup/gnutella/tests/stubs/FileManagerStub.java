package com.limegroup.gnutella.tests.stubs;

import com.limegroup.gnutella.*;
import java.io.*;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
public class FileManagerStub extends FileManager {
    public FileDesc get(int i) {
        return new FileDesc(i, "fake.txt", "z:/fake.txt", Integer.MAX_VALUE);
    }
    public InputStream getInputStream(FileDesc ignored) {
        return new InputStream() {
            public int read() {
                return 'a';
            }
        };
    }
}
