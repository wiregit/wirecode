package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.QueryRequest;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * A file manager that behaves exactly like FileManager would if
 * MetaFileManager didn't exist.
 */
public class SimpleFileManager extends FileManager {
    
    public boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return false;
    }
    
    public void addXMLToResponse(Response r, FileDesc fd) {
        ;
    }
    
    public FileDesc addFileIfShared(File f, List docs, long creationTime) {
        return addFileIfShared(f);
    }
}

