package com.limegroup.gnutella.uploader;

public interface HttpRequestHandlerFactory {

    public FileRequestHandler createFileRequestHandler();

    public BrowseRequestHandler createBrowseRequestHandler();

    public FreeLoaderRequestHandler createFreeLoaderRequestHandler();

    // TODO move LimitReachedRequestHandler into FileRequestHandler
    public LimitReachedRequestHandler createLimitReachedRequestHandler(HTTPUploader uploader);
    
    public HttpPushRequestHandler createPushProxyRequestHandler();

}