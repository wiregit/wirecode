package com.limegroup.gnutella.stubs;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerController;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnCache;
import com.limegroup.gnutella.UrnCallback;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.xml.LimeXMLSchema;

public class FileManagerControllerAdapter implements FileManagerController {

    UrnCache urnCache = new UrnCache();
    
    public void add(String string,
            LimeXMLReplyCollection createLimeXMLReplyCollection) {
        // TODO Auto-generated method stub

    }

    public void addSimppListener(SimppListener listener) {
        // TODO Auto-generated method stub

    }

    public void addUrns(File file, Set<? extends URN> urns) {
        // TODO Auto-generated method stub

    }

    public void calculateAndCacheUrns(File file, UrnCallback callback) {
        urnCache.calculateAndCacheUrns(file, callback);
    }

    public void clearPendingShare(File f) {
        // TODO Auto-generated method stub

    }

    public LimeXMLDocument createLimeXMLDocument(
            Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI) {
        // TODO Auto-generated method stub
        return null;
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        // TODO Auto-generated method stub
        return null;
    }

    public Response createPureMetadataResponse() {
        // TODO Auto-generated method stub
        return null;
    }

    public Response createResponse(FileDesc desc) {
        // TODO Auto-generated method stub
        return null;
    }

    public void fileAdded(File file, URN urn) {
        // TODO Auto-generated method stub

    }

    public void fileChanged(URN urn, Long time) {
        // TODO Auto-generated method stub

    }

    public void fileManagerLoading() {
        // TODO Auto-generated method stub

    }

    public int getAlternateLocationCount(URN urn) {
        // TODO Auto-generated method stub
        return 0;
    }

    public String[] getAvailableSchemaURIs() {
        // TODO Auto-generated method stub
        return null;
    }

    public Collection<LimeXMLReplyCollection> getCollections() {
        // TODO Auto-generated method stub
        return null;
    }

    public Long getCreationTime(URN urn) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<URN> getNewestUrns(QueryRequest qr, int number) {
        // TODO Auto-generated method stub
        return null;
    }

    public LimeXMLReplyCollection getReplyCollection(String string) {
        // TODO Auto-generated method stub
        return null;
    }

    public ContentResponseData getResponseDataFor(URN urn) {
        // TODO Auto-generated method stub
        return null;
    }

    public LimeXMLSchema getSchema(String audioSchema) {
        // TODO Auto-generated method stub
        return null;
    }

    public void handleSharedFileUpdate(File file) {
        // TODO Auto-generated method stub

    }

    public void lastUrnRemoved(URN urn) {
        // TODO Auto-generated method stub

    }

    public void loadFinished() {
        // TODO Auto-generated method stub

    }

    public void loadFinishedPostSave() {
        // TODO Auto-generated method stub

    }

    public void loadStarted() {
        // TODO Auto-generated method stub

    }

    public LimeXMLDocument readDocument(File file) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeSimppListener(SimppListener listener) {
        // TODO Auto-generated method stub

    }

    public void requestValidation(URN urn, ContentResponseObserver observer) {
        // TODO Auto-generated method stub

    }

    public void save() {
        // TODO Auto-generated method stub

    }

    public void scheduleWithFixedDelay(Runnable command, int initialDelay,
            int delay, TimeUnit unit) {
        // TODO Auto-generated method stub

    }

    public void setAnnotateEnabled(boolean enabled) {
        // TODO Auto-generated method stub

    }

    public boolean warnAboutSharingSensitiveDirectory(File directory) {
        // TODO Auto-generated method stub
        return false;
    }

}
