package com.limegroup.gnutella;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.messages.QueryRequest;

public interface FileManagerController {

    public void save();

    public void loadStarted();

    public void loadFinished();

    public void clearPendingShare(File f);

    public void calculateAndCacheUrns(File file, UrnCallback callback);

    public void addUrns(File file, Set<? extends URN> urns);

    public void fileAdded(File file, URN urn);

    public void lastUrnRemoved(URN urn);

    public List<URN> getNewestUrns(QueryRequest qr, int number);

    public ContentResponseData getResponseDataFor(URN urn);

    public void requestValidation(URN urn, ContentResponseObserver observer);

    public int getAlternateLocationCount(URN urn);

    public Long getCreationTime(URN urn);

    public void fileChanged(URN urn, Long time);

    public Response createResponse(FileDesc desc);

    public Response createPureMetadataResponse();

}