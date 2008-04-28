package com.limegroup.gnutella.stubs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.limewire.util.NameValue;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerController;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;


/**
 * A file manager that behaves exactly like FileManager would if
 * MetaFileManager didn't exist.
 */
@Singleton
public class SimpleFileManager extends FileManagerImpl {
    LimeXMLDocumentFactory factory;
    
    @Inject
    public SimpleFileManager(FileManagerController fileManagerController, 
            LimeXMLDocumentFactory factory) {
        super(fileManagerController);
        this.factory = factory;
    }
    
    public SimpleFileManager() {
        super(new FileManagerControllerAdapter());
    }

    @Override
    public boolean shouldIncludeXMLInResponse(QueryRequest qr) {
        return false;
    }
    
    @Override
    public void addXMLToResponse(Response r, FileDesc fd) {
        r.setDocument(fd.getXMLDocument());
    }
    
    @Override
    public void fileChanged(File f) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public boolean isValidXMLMatch(Response r, LimeXMLDocument doc) {
        return true;
    }
    
    /**
     * Override the fd to create a fake fd for store files so we dont 
     *	need to read real store files for tests
     */
    @Override
    protected void loadFile(FileDesc fd, File file,
            List<? extends LimeXMLDocument> metadata, Set<? extends URN> urns) {
        
        if( file.getName().contains("store")) {

            List<NameValue<String>> id3List = new ArrayList<NameValue<String>>();
            id3List.add( new NameValue<String>("audios__audio__licensetype__", "LIMEWIRE_STORE_PURCHASE"));
            
            LimeXMLDocument doc = factory.createLimeXMLDocument(id3List, LimeXMLNames.AUDIO_SCHEMA);
            fd.addLimeXMLDocument(doc);
        }
    }

}

