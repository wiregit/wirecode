package org.limewire.facebook.service.livemessage;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.facebook.service.FacebookFriendConnection;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FileOfferHandler implements LiveMessageHandler, FeatureTransport<FileMetaData>{

    private static final String TYPE = "file-offer";

    private final FacebookFriendConnection connection;
    private final Handler<FileMetaData> fileMetaDataHandler;

    @AssistedInject
    FileOfferHandler(@Assisted FacebookFriendConnection connection,
                     FeatureTransport.Handler<FileMetaData> fileMetaDataHandler) {
        this.connection = connection;
        this.fileMetaDataHandler = fileMetaDataHandler;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        fileMetaDataHandler.featureReceived(null, null);  // TODO
    }

    @Override
    public void sendFeature(FriendPresence presence, FileMetaData localFeature) throws FriendException {
        Map<String, Object> messageMap = new HashMap<String, Object>(); // TODO
        connection.sendLiveMessage(presence, TYPE, null);
    }
}
