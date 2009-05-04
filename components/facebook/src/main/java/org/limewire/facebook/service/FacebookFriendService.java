package org.limewire.facebook.service;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;
import org.json.JSONException;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FacebookFriendService {
    private final ThreadPoolListeningExecutor executorService;
    private FacebookFriendConnection connection;
    private final ChatClientFactory chatClientFactory;
    private final AddressSender addressSender;

    @Inject FacebookFriendService(FacebookFriendConnection connection,
                                  ChatClientFactory chatClientFactory,
                                  AddressSender addressSender){
        this.connection = connection;
        this.chatClientFactory = chatClientFactory;
        this.addressSender = addressSender;
        executorService = ExecutorsHelper.newSingleThreadExecutor(ExecutorsHelper.daemonThreadFactory(getClass().getSimpleName()));    
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
        addressSender.start();
        return connection;
    }
}
