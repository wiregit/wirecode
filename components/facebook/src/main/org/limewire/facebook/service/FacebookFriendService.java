package org.limewire.facebook.service;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ThreadPoolListeningExecutor;

import com.google.code.facebookapi.FacebookException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class FacebookFriendService {
    private final ThreadPoolListeningExecutor executorService;
    private FacebookFriendConnection connection;
    private final SessionFactory sessionFactory;
    private final ChatClientFactory chatClientFactory;
    private final AddressSenderFactory addressSenderFactory;

    @Inject FacebookFriendService(FacebookFriendConnection connection,
                                  SessionFactory sessionFactory,
                                  ChatClientFactory chatClientFactory,
                                  AddressSenderFactory addressSenderFactory){
        this.connection = connection;
        this.sessionFactory = sessionFactory;
        this.chatClientFactory = chatClientFactory;
        this.addressSenderFactory = addressSenderFactory;
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
    
    FacebookFriendConnection loginImpl() throws IOException, FacebookException {
        connection.loginImpl();
        ChatClient client = chatClientFactory.createChatClient(connection);
        client.start();
        AddressSender addressSender = addressSenderFactory.create(sessionFactory.getSecret(connection.getSession()));
        return connection;
    }
}
