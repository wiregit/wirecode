package com.limegroup.gnutella.chat;

import java.net.Socket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.util.SocketsManager;

@Singleton
public class InstantMessengerFactoryImpl implements InstantMessengerFactory {

    private final Provider<SocketsManager> socketsManager;
    private final Provider<ChatManager> chatManager;
    private final Provider<ActivityCallback> activityCallback;

    @Inject
    public InstantMessengerFactoryImpl(Provider<ChatManager> chatManager, Provider<ActivityCallback> activityCallback, Provider<SocketsManager> socketsManager) {
        this.chatManager = chatManager;
        this.activityCallback = activityCallback;
        this.socketsManager = socketsManager;
    }
    
    public InstantMessenger createIncomingInstantMessenger(Socket socket) {
        return new InstantMessenger(socket, chatManager.get(), activityCallback.get());
    }

    public InstantMessenger createOutgoingInstantMessenger(String host, int port) {
        return new InstantMessenger(host, port, chatManager.get(), activityCallback.get(), socketsManager.get());
    }

}
