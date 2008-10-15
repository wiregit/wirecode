package com.limegroup.gnutella.uploader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

@Singleton
public class HttpRequestHandlerFactoryImpl implements HttpRequestHandlerFactory {
    
    private final Provider<FileRequestHandlerFactoryImpl> fileRequestHandlerProvider;
    private final Provider<FreeLoaderRequestHandler> freeLoaderRequestHandlerProvider;
    private final Provider<HttpPushRequestHandler> pushProxyRequestHandlerProvider;
    
    private final HTTPHeaderUtils httpHeaderUtils;
    private final AltLocManager altLocManager;
    private final Provider<BrowseRequestHandlerFactory> browseRequestHandlerFactory;
    
    @Inject
    public HttpRequestHandlerFactoryImpl(
            Provider<FileRequestHandlerFactoryImpl> fileRequestHandlerProvider,
            Provider<FreeLoaderRequestHandler> freeLoaderRequestHandlerProvider,
            Provider<HttpPushRequestHandler> pushProxyRequestHandlerProvider,
            HTTPHeaderUtils httpHeaderUtils,
            AltLocManager altLocManager,
            Provider<BrowseRequestHandlerFactory> browseRequestHandlerFactory) {
        this.fileRequestHandlerProvider = fileRequestHandlerProvider;
        this.freeLoaderRequestHandlerProvider = freeLoaderRequestHandlerProvider;
        this.pushProxyRequestHandlerProvider = pushProxyRequestHandlerProvider;
        this.httpHeaderUtils = httpHeaderUtils;
        this.altLocManager = altLocManager;
        this.browseRequestHandlerFactory = browseRequestHandlerFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.uploader.HttpRequestHandlerFactory#createFileRequestHandler()
     */
    public FileRequestHandler createFileRequestHandler(HttpRequestFileListProvider provider) {
        return fileRequestHandlerProvider.get().createFileRequestHandler(provider);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.uploader.HttpRequestHandlerFactory#createBrowseRequestHandler()
     */
    @Override
    public BrowseRequestHandler createBrowseRequestHandler(HttpRequestFileListProvider browseRequestFileListProvider) {
        return browseRequestHandlerFactory.get().createBrowseRequestHandler(browseRequestFileListProvider);
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
