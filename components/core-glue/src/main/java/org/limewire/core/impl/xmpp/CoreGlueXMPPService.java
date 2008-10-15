package org.limewire.core.impl.xmpp;

import org.limewire.i18n.I18nMarker;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;

import com.google.inject.Provider;
import com.google.inject.Singleton;

import com.google.inject.Inject;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.uploader.BrowseRequestHandlerFactory;

@Singleton
public class CoreGlueXMPPService implements Service {

    @Inject
    private final Provider<HTTPAcceptor> httpAcceptor;
    private final BrowseRequestHandlerFactory browseRequestHandlerFactory;
    private final Provider<AuthenticatingBrowseFriendListProvider> authenticatingBrowseFriendListProvider;
    
    private final static String FRIEND_BROWSE_PATTERN = "/friend/browse/*";

    public CoreGlueXMPPService(Provider<HTTPAcceptor> httpAcceptor, BrowseRequestHandlerFactory browseRequestHandlerFactory,
            Provider<AuthenticatingBrowseFriendListProvider> authenticatingBrowseFriendListProvider) {
        this.httpAcceptor = httpAcceptor;
        this.browseRequestHandlerFactory = browseRequestHandlerFactory;
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
        httpAcceptor.get().registerHandler(FRIEND_BROWSE_PATTERN, browseRequestHandlerFactory.createBrowseRequestHandler(authenticatingBrowseFriendListProvider.get())); 
    }

    @Override
    public void stop() {
        httpAcceptor.get().unregisterHandler(FRIEND_BROWSE_PATTERN);
    }

}
