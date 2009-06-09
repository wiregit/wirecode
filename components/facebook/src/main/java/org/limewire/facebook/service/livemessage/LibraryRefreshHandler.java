package org.limewire.facebook.service.livemessage;

import java.util.HashMap;

import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.core.api.friend.feature.features.LibraryChangedNotifier;
import org.limewire.facebook.service.FacebookFriendConnection;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class LibraryRefreshHandler implements FeatureTransport<LibraryChangedNotifier>, LiveMessageHandler {
    
    private static final String TYPE = "library-changed";
    
    private final FacebookFriendConnection connection;
    private final Handler<LibraryChangedNotifier> handler;

    @AssistedInject
    public LibraryRefreshHandler(@Assisted FacebookFriendConnection connection,
                                              Handler<LibraryChangedNotifier> handler) {
        this.connection = connection;
        this.handler = handler;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) {
        String from = message.optString("from", null);
        handler.featureReceived(from, new LibraryChangedNotifier(){});
    }

    @Override
    public void sendFeature(FriendPresence presence, LibraryChangedNotifier localFeature) throws FriendException {
        connection.sendLiveMessage(presence, TYPE, new HashMap<String, Object>());
    }
}
