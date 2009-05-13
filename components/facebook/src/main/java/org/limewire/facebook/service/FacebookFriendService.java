package org.limewire.facebook.service;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.core.api.friend.client.FriendConnectionFactoryRegistry;
import org.limewire.xmpp.api.client.XMPPConnection;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FacebookFriendService implements FriendConnectionFactory {
    private final ThreadPoolListeningExecutor executorService;
    private FacebookFriendConnection connection;
    private final ChatClientFactory chatClientFactory;

    @Inject FacebookFriendService(FacebookFriendConnection connection,
                                  ChatClientFactory chatClientFactory){
        this.connection = connection;
        this.chatClientFactory = chatClientFactory;
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName()));    
    }

    @Override
    public ListeningFuture<XMPPConnection> login(FriendConnectionConfiguration configuration) {
        return null;
    }

    @Override
    @Inject
    public void register(FriendConnectionFactoryRegistry registry) {
        registry.register(Network.Type.FACEBOOK, this);
    }

    public ListeningFuture<FacebookFriendConnection> login() {
        return executorService.submit(new Callable<FacebookFriendConnection>() {
            @Override
            public FacebookFriendConnection call() throws Exception {
                return loginImpl();
            }
        });
    }
    
    FacebookFriendConnection loginImpl() throws IOException, FacebookException, JSONException {
        connection.loginImpl();
        ChatClient client = chatClientFactory.createChatClient(connection);
        client.start();
        return connection;
    }
}
