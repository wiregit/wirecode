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

    }

    public void addSimppListener(SimppListener listener) {

    }

    public void addUrns(File file, Set<? extends URN> urns) {

    }

    public void calculateAndCacheUrns(File file, UrnCallback callback) {
        urnCache.calculateAndCacheUrns(file, callback);
    }

    public void clearPendingShare(File f) {

    }

    public LimeXMLDocument createLimeXMLDocument(
            Collection<? extends Entry<String, String>> nameValueList,
            String schemaURI) {
        return null;
    }

    public LimeXMLReplyCollection createLimeXMLReplyCollection(String URI) {
        return null;
    }

    public Response createPureMetadataResponse() {
        return null;
    }

    public Response createResponse(FileDesc desc) {
        return null;
    }

    public void fileAdded(File file, URN urn) {

    }

    public void fileChanged(URN urn, Long time) {

    }

    public void fileManagerLoading() {

    }

    public int getAlternateLocationCount(URN urn) {
        return 0;
    }

    public String[] getAvailableSchemaURIs() {
        return null;
    }

    public Collection<LimeXMLReplyCollection> getCollections() {
        return null;
    }

    public Long getCreationTime(URN urn) {
        return null;
    }

    public List<URN> getNewestSharedUrns(QueryRequest qr, int number) {
        return null;
    }

    public LimeXMLReplyCollection getReplyCollection(String string) {
        return null;
    }

    public ContentResponseData getResponseDataFor(URN urn) {
        return null;
    }

    public LimeXMLSchema getSchema(String audioSchema) {
        return null;
    }

    public void handleSharedFileUpdate(File file) {

    }

    public void lastUrnRemoved(URN urn) {

    }

    public void loadFinished() {

    }

    public void loadFinishedPostSave() {

    }

    public void loadStarted() {

    }

    public LimeXMLDocument readDocument(File file) throws IOException {
        return null;
    }

    public void removeSimppListener(SimppListener listener) {

    }

    public void requestValidation(URN urn, ContentResponseObserver observer) {

    }

    public void save() {

    }

    public void scheduleWithFixedDelay(Runnable command, int initialDelay,
            int delay, TimeUnit unit) {

    }

    public void setAnnotateEnabled(boolean enabled) {

    }

    public boolean warnAboutSharingSensitiveDirectory(File directory) {
        return false;
    }

}
