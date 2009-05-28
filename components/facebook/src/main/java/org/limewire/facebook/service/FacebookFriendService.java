package org.limewire.facebook.service;

import java.util.concurrent.Callable;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.impl.LimewireFeatureInitializer;
import org.limewire.facebook.service.livemessage.DiscoInfoHandlerFactory;
import org.limewire.facebook.service.livemessage.PresenceHandlerFactory;
import org.limewire.lifecycle.Asynchronous;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FacebookFriendService implements FriendConnectionFactory, Service {
    
    private static Log LOG = LogFactory.getLog(FacebookFriendService.class);
    
    private final ThreadPoolListeningExecutor executorService;
    private final FacebookFriendConnectionFactory connectionFactory;

    private final DiscoInfoHandlerFactory liveDiscoInfoHandlerFactory;
    private final PresenceHandlerFactory presenceHandlerFactory;
    private final FeatureRegistry featureRegistry;
    private volatile ChatListener listener;
    private volatile FacebookFriendConnection connection;

    @Inject FacebookFriendService(FacebookFriendConnectionFactory connectionFactory,
                                  DiscoInfoHandlerFactory liveDiscoInfoHandlerFactory,
                                  PresenceHandlerFactory presenceHandlerFactory,
                                  FeatureRegistry featureRegistry) {
        this.connectionFactory = connectionFactory;
        this.liveDiscoInfoHandlerFactory = liveDiscoInfoHandlerFactory;
        this.presenceHandlerFactory = presenceHandlerFactory;
        this.featureRegistry = featureRegistry;
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName()));    
    }
    
    @Inject
    void register(ServiceRegistry registry) {
        registry.register(this);
    }
    
    public void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                if(event.getType() == FriendConnectionEvent.Type.DISCONNECTED) {
                    synchronized (FacebookFriendService.this) {
                        if(connection != null && connection == event.getSource()) {
                            connection = null;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void start() {
        
    }

    @Override
    @Asynchronous
    public void stop() {
        logoutImpl();
    }

    @Override
    public void initialize() {
        
    }

    @Override
    public String getServiceName() {
        return getClass().getSimpleName();
    }
    
    private void logoutImpl() {
        synchronized (this) {
            if(connection != null) {
                connection.logoutImpl();
                connection = null;
            }
        }
    }

    @Override
    public ListeningFuture<FriendConnection> login(final FriendConnectionConfiguration configuration) {
        return executorService.submit(new Callable<FriendConnection>() {
            @Override
            public FriendConnection call() throws Exception {
                return loginImpl(configuration);
            }
        });
    }

    @Override
    @Inject
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.FACEBOOK, this);
    }
    
    FacebookFriendConnection loginImpl(FriendConnectionConfiguration configuration) throws FriendException {
        LOG.debug("creating connection");
        connection = connectionFactory.create(configuration);
        liveDiscoInfoHandlerFactory.create(connection);
        presenceHandlerFactory.create(connection);
        new LimewireFeatureInitializer().register(featureRegistry);
        LOG.debug("logging in to facebook...");
        connection.loginImpl();
        LOG.debug("logged in.");
        return connection;
    }
}
