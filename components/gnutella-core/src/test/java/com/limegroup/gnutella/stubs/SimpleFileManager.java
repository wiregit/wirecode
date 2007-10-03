package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.NameValue;
import org.xml.sax.SAXException;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerController;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactoryImpl;
import com.limegroup.gnutella.xml.LimeXMLNames;


/**
 * A file manager that behaves exactly like FileManager would if
 * MetaFileManager didn't exist.
 */
@Singleton
public class SimpleFileManager extends FileManager {
    LimeXMLDocumentFactory factory;
    protected Injector injector;
    
    @Inject
    public SimpleFileManager(FileManagerController fileManagerController) {
        super(fileManagerController);
        
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(LimeXMLDocumentFactory.class).to(LimeXMLDocumentFactoryImpl.class);
            }
        });
        factory = injector.getInstance(LimeXMLDocumentFactory.class);
    }
    
    public SimpleFileManager() {
        super(new FileManagerControllerAdapter());
    }

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
    
    /**
     * Override the fd to create a fake fd for store files so we dont 
     *	need to read real store files for tests
     */
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

