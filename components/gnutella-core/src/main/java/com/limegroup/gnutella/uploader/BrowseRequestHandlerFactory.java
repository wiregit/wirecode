package com.limegroup.gnutella.uploader;

import org.limewire.core.api.browse.server.BrowseTracker;
import org.limewire.http.auth.RequiresAuthentication;
import org.limewire.xmpp.api.client.XMPPService;

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
    private final BrowseTracker tracker;
    private final XMPPService xmppService;

    @Inject
    public BrowseRequestHandlerFactory(HTTPUploadSessionManager sessionManager,
            Provider<ResponseFactory> responseFactory,
            OutgoingQueryReplyFactory outgoingQueryReplyFactory,
            BrowseTracker tracker,
            XMPPService xmppService) {
        this.sessionManager = sessionManager;
        this.responseFactory = responseFactory;
        this.outgoingQueryReplyFactory = outgoingQueryReplyFactory;
        this.tracker = tracker;
        this.xmppService = xmppService;
    }
    
    public BrowseRequestHandler createBrowseRequestHandler(HttpRequestFileListProvider browseRequestFileListProvider,
                                                           boolean requiresAuthentication) {
        if(!requiresAuthentication) {
            return new BrowseRequestHandler(sessionManager, responseFactory, outgoingQueryReplyFactory,
                    browseRequestFileListProvider, tracker, xmppService);
        } else {
            return new ProtectedBrowseRequestHandler(sessionManager, responseFactory, outgoingQueryReplyFactory,
                    browseRequestFileListProvider);
        }
    }
    
    @RequiresAuthentication 
    class ProtectedBrowseRequestHandler extends BrowseRequestHandler {
        ProtectedBrowseRequestHandler(HTTPUploadSessionManager sessionManager, Provider<ResponseFactory> responseFactory, OutgoingQueryReplyFactory outgoingQueryReplyFactory, HttpRequestFileListProvider browseRequestFileListProvider) {
            super(sessionManager, responseFactory, outgoingQueryReplyFactory, browseRequestFileListProvider, tracker, xmppService);
        }
    }

}
