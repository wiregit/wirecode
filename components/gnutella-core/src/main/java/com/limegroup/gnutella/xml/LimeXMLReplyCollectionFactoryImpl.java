package com.limegroup.gnutella.xml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.metadata.MetaDataReader;

@Singleton
public class LimeXMLReplyCollectionFactoryImpl implements
        LimeXMLReplyCollectionFactory {

    private final Provider<LimeXMLProperties> limeXMLProperties;

    private final Provider<FileManager> fileManager;

    private final LimeXMLDocumentFactory limeXMLDocumentFactory;

    private final MetaDataReader metaDataReader;
    
    @Inject
    public LimeXMLReplyCollectionFactoryImpl(
            Provider<LimeXMLProperties> limeXMLProperties, Provider<FileManager> fileManager,
            LimeXMLDocumentFactory limeXMLDocumentFactory, MetaDataReader metaDataReader) {
        this.limeXMLProperties = limeXMLProperties;
        this.fileManager = fileManager;
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
        this.metaDataReader = metaDataReader;

    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI,
            String path) {
        return new LimeXMLReplyCollection(URI, path, fileManager, limeXMLDocumentFactory, metaDataReader);
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        return createLimeXMLReplyCollection(URI, limeXMLProperties.get().getXMLDocsDir());
    }

}
