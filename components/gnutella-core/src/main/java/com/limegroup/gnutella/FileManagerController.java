package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLSchema;

public interface FileManagerController {

    public void save();

    public void loadStarted();

    public void loadFinished();

    public void clearPendingShare(File f);

    public void calculateAndCacheUrns(File file, UrnCallback callback);

    public void addUrns(File file, Set<? extends URN> urns);

    public void fileAdded(File file, URN urn);

    public void lastUrnRemoved(URN urn);

    /**
     * NOTE: this is only returning newest URNs of shared files, store
     * file are ignored here even if they are part of the newest files
     */
    public List<URN> getNewestSharedUrns(QueryRequest qr, int number);

    public ContentResponseData getResponseDataFor(URN urn);

    public void requestValidation(URN urn, ContentResponseObserver observer);

    public int getAlternateLocationCount(URN urn);

    public Long getCreationTime(URN urn);

    public void fileChanged(URN urn, Long time);

    public Response createResponse(FileDesc desc);

    public Response createPureMetadataResponse();

    public void loadFinishedPostSave();

    public void addSimppListener(SimppListener listener);
    
    public void removeSimppListener(SimppListener listener);

    public void fileManagerLoading();

    public boolean warnAboutSharingSensitiveDirectory(File directory);

    public void handleSharedFileUpdate(File file);

    public void scheduleWithFixedDelay(Runnable command, int initialDelay, int delay, TimeUnit unit);

    public void setAnnotateEnabled(boolean enabled);
    
    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI);
    
    public LimeXMLDocument createLimeXMLDocument(Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI);
    
    public LimeXMLDocument readDocument(File file) throws IOException;

    public void add(String string,
            LimeXMLReplyCollection createLimeXMLReplyCollection);

    public Collection<LimeXMLReplyCollection> getCollections();

    public LimeXMLReplyCollection getReplyCollection(String string);

    public String[] getAvailableSchemaURIs();

    public LimeXMLSchema getSchema(String audioSchema);

}