package com.limegroup.gnutella.uploader;

public interface HttpRequestHandlerFactory {

    public FileRequestHandler createFileRequestHandler();

    public BrowseRequestHandler createBrowseRequestHandler();

    public FreeLoaderRequestHandler createFreeLoaderRequestHandler();

    public LimitReachedRequestHandler createLimitReachedRequestHandler(HTTPUploader uploader);
    
    public PushProxyRequestHandler createPushProxyRequestHandler();

}