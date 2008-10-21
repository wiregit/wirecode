package org.limewire.core.impl.xmpp;

import org.limewire.i18n.I18nMarker;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;

@Singleton
public class CoreGlueXMPPService implements Service {

    private final Provider<HTTPAcceptor> httpAcceptor;
    private final HttpRequestHandlerFactory httpRequestHandlerFactory;
    private final Provider<FriendFileListProvider> authenticatingBrowseFriendListProvider;
    
    private final static String FRIEND_BROWSE_PATTERN = "/friend/browse";
    private final static String FRIEND_DOWNLOAD_PATTERN = "/friend/download";

    @Inject
    public CoreGlueXMPPService(Provider<HTTPAcceptor> httpAcceptor, HttpRequestHandlerFactory httpRequestHandlerFactory,
           Provider<FriendFileListProvider> authenticatingBrowseFriendListProvider) {
        this.httpAcceptor = httpAcceptor;
        this.httpRequestHandlerFactory = httpRequestHandlerFactory;
        this.authenticatingBrowseFriendListProvider = authenticatingBrowseFriendListProvider;
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    @Override
    public String getServiceName() {
        return I18nMarker.marktr("XMPP Service");
    }

    @Override
    public void initialize() {
    }

    @Override
    public void start() {
        httpAcceptor.get().registerHandler(FRIEND_BROWSE_PATTERN, httpRequestHandlerFactory.createBrowseRequestHandler(authenticatingBrowseFriendListProvider.get(), true)); 
        httpAcceptor.get().registerHandler(FRIEND_DOWNLOAD_PATTERN, httpRequestHandlerFactory.createFileRequestHandler(authenticatingBrowseFriendListProvider.get(), true));
    }

    @Override
    public void stop() {
        httpAcceptor.get().unregisterHandler(FRIEND_BROWSE_PATTERN);
    }

}
