package com.limegroup.gnutella.uploader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.altlocs.AltLocManager;

@Singleton
public class HttpRequestHandlerFactoryImpl implements HttpRequestHandlerFactory {
    
    private final Provider<FileRequestHandler> fileRequestHandlerProvider;
    private final Provider<BrowseRequestHandler> browseRequestHandlerProvider;
    private final Provider<FreeLoaderRequestHandler> freeLoaderRequestHandlerProvider;
    private final Provider<HttpPushRequestHandler> pushProxyRequestHandlerProvider;
    
    private final HTTPHeaderUtils httpHeaderUtils;
    private final AltLocManager altLocManager;
    
    @Inject
    public HttpRequestHandlerFactoryImpl(
            Provider<FileRequestHandler> fileRequestHandlerProvider,
            Provider<BrowseRequestHandler> browseRequestHandlerProvider,
            Provider<FreeLoaderRequestHandler> freeLoaderRequestHandlerProvider,
            Provider<HttpPushRequestHandler> pushProxyRequestHandlerProvider,
            HTTPHeaderUtils httpHeaderUtils,
            AltLocManager altLocManager) {
        this.fileRequestHandlerProvider = fileRequestHandlerProvider;
        this.browseRequestHandlerProvider = browseRequestHandlerProvider;
        this.freeLoaderRequestHandlerProvider = freeLoaderRequestHandlerProvider;
        this.pushProxyRequestHandlerProvider = pushProxyRequestHandlerProvider;
        this.httpHeaderUtils = httpHeaderUtils;
        this.altLocManager = altLocManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.uploader.HttpRequestHandlerFactory#createFileRequestHandler()
     */
    public FileRequestHandler createFileRequestHandler() {
        return fileRequestHandlerProvider.get();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.uploader.HttpRequestHandlerFactory#createBrowseRequestHandler()
     */
    public BrowseRequestHandler createBrowseRequestHandler() {
        return browseRequestHandlerProvider.get();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.uploader.HttpRequestHandlerFactory#createFreeLoaderRequestHandler()
     */
    public FreeLoaderRequestHandler createFreeLoaderRequestHandler() {
        return freeLoaderRequestHandlerProvider.get();
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.uploader.HttpRequestHandlerFactory#createLimitReachedRequestHandler(com.limegroup.gnutella.uploader.HTTPUploader)
     */
    public LimitReachedRequestHandler createLimitReachedRequestHandler(HTTPUploader uploader) {
        return new LimitReachedRequestHandler(uploader, httpHeaderUtils, altLocManager);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.uploader.HttpRequestHandlerFactory#createPushProxyRequestHandler()
     */
    public HttpPushRequestHandler createPushProxyRequestHandler() {
        return pushProxyRequestHandlerProvider.get();
    }

 

}
