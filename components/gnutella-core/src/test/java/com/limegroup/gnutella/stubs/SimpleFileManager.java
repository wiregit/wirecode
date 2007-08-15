package com.limegroup.gnutella.stubs;

import java.io.File;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A file manager that behaves exactly like FileManager would if
 * MetaFileManager didn't exist.
 */
public class SimpleFileManager extends FileManager {
    
    public boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return false;
    }
    
    public void addXMLToResponse(Response r, FileDesc fd) {
        r.setDocument(fd.getXMLDocument());
    }
    
    public void fileChanged(File f) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    public boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return true;
    }
}

