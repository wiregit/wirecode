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
    
    private static LimeXMLDocument document;
    
    public boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return false;
    }
    
    public void addXMLToResponse(Response r, FileDesc fd) {
        if (document == null) {
            try {
                document = new LimeXMLDocument(
                        "<?xml version=\"1.0\"?>"+
                        "<audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\">"+
                        "  <audio genre=\"Rock\" identifier=\"def1.txt\" bitrate=\"190\"/>"+
                        "</audios>");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
        r.setDocument(document);
    }
    
    public void fileChanged(File f) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    public boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return true;
    }
}

