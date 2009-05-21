package org.limewire.facebook.service;

import java.util.concurrent.Callable;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.impl.LimewireFeatureInitializer;
import org.limewire.core.api.friend.client.FriendConnection;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FacebookFriendService implements FriendConnectionFactory {
    
    private static Log LOG = LogFactory.getLog(FacebookFriendService.class);
    
    private final ThreadPoolListeningExecutor executorService;
    private final FacebookFriendConnectionFactory connectionFactory;
    private final ChatClientFactory chatClientFactory;

    private final LiveMessageDiscoInfoTransportFactory liveDiscoInfoTransportFactory;
    private final FeatureRegistry featureRegistry;

    @Inject FacebookFriendService(FacebookFriendConnectionFactory connectionFactory,
                                  ChatClientFactory chatClientFactory,
                                  LiveMessageDiscoInfoTransportFactory liveDiscoInfoTransportFactory,
                                  FeatureRegistry featureRegistry) {
        this.connectionFactory = connectionFactory;
        this.chatClientFactory = chatClientFactory;
        this.liveDiscoInfoTransportFactory = liveDiscoInfoTransportFactory;
        this.featureRegistry = featureRegistry;
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName()));    
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
        FacebookFriendConnection connection = connectionFactory.create(configuration);
        liveDiscoInfoTransportFactory.create(connection);
        new LimewireFeatureInitializer().register(featureRegistry);
        LOG.debug("logging in");
        connection.loginImpl();
        ChatClient client = chatClientFactory.createChatClient(connection);
        client.start();
        return connection;
    }
}
