package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*; 
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A simple FileManager that shares one file of (near) infinite length.
 */
public class FileManagerStub extends FileManager {

    FileDescStub fdStub = new FileDescStub();

    public FileDesc get(int i) {
        return fdStub;
    }
}

