package com.limegroup.gnutella.xml;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileManager;

@Singleton
public class LimeXMLReplyCollectionFactoryImpl implements
        LimeXMLReplyCollectionFactory {

    private final Provider<LimeXMLProperties> limeXMLProperties;

    private final Provider<FileManager> fileManager;

    @Inject
    public LimeXMLReplyCollectionFactoryImpl(
            Provider<LimeXMLProperties> limeXMLProperties, Provider<FileManager> fileManager) {
        this.limeXMLProperties = limeXMLProperties;
        this.fileManager = fileManager;

    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI,
            String path) {
        return new LimeXMLReplyCollection(URI, path, fileManager);
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        return createLimeXMLReplyCollection(URI, limeXMLProperties.get().getXMLDocsDir());
    }

}
