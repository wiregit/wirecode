package com.limegroup.gnutella.uploader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ResponseFactory;
import com.limegroup.gnutella.messages.OutgoingQueryReplyFactory;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

@Singleton
public class BrowseRequestHandlerFactory {

    private final HTTPUploadSessionManager sessionManager;
    private final Provider<ResponseFactory> responseFactory;
    private final OutgoingQueryReplyFactory outgoingQueryReplyFactory;

    @Inject
    public BrowseRequestHandlerFactory(HTTPUploadSessionManager sessionManager,
            Provider<ResponseFactory> responseFactory,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory) {
        this.sessionManager = sessionManager;
        this.responseFactory = responseFactory;
        this.outgoingQueryReplyFactory = outgoingQueryReplyFactory;
    }
    
    public BrowseRequestHandler createBrowseRequestHandler(HttpRequestFileListProvider browseRequestFileListProvider) {
        return new BrowseRequestHandler(sessionManager, responseFactory, outgoingQueryReplyFactory,
                browseRequestFileListProvider);
    }

}
