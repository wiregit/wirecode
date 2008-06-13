package com.limegroup.gnutella.stubs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerImpl;
import com.limegroup.gnutella.SavedFileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLReplyCollectionFactory;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaReplyCollectionMapper;

/**
 * A file manager that behaves exactly like FileManager would if
 * MetaFileManager didn't exist.
 */
@Singleton
public class SimpleFileManager extends FileManagerImpl {
    LimeXMLDocumentFactory factory;
    
    @Inject
     public SimpleFileManager(LimeXMLDocumentFactory factory,
            Provider<SimppManager> simppManager,
            Provider<UrnCache> urnCache,
            Provider<DownloadManager> downloadManager,
            Provider<CreationTimeCache> creationTimeCache,
            Provider<ContentManager> contentManager,
            Provider<AltLocManager> altLocManager,
            Provider<SavedFileManager> savedFileManager,
            Provider<UpdateHandler> updateHandler,
            Provider<ActivityCallback> activityCallback,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            LimeXMLReplyCollectionFactory limeXMLReplyCollectionFactory,
            LimeXMLDocumentFactory limeXMLDocumentFactory,
            MetaDataReader metaDataReader,
            Provider<SchemaReplyCollectionMapper> schemaReplyCollectionMapper,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        super(simppManager, urnCache, downloadManager, creationTimeCache, contentManager, altLocManager, savedFileManager, updateHandler, activityCallback, backgroundExecutor, limeXMLReplyCollectionFactory, limeXMLDocumentFactory, metaDataReader, schemaReplyCollectionMapper, limeXMLSchemaRepository);
        this.factory = factory;
    }
    
    public SimpleFileManager() {
        super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public void fileChanged(File f) {
        throw new UnsupportedOperationException("unsupported");
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

