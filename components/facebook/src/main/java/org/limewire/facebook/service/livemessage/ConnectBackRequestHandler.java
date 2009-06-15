package org.limewire.facebook.service.livemessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.ConnectBackRequestedEvent;
import org.limewire.net.address.ConnectableSerializer;
import org.limewire.util.StringUtils;
import org.limewire.facebook.service.FacebookFriendConnection;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ConnectBackRequestHandler implements FeatureTransport<ConnectBackRequest>,
        LiveMessageHandler {

    private static final Log LOG = LogFactory.getLog(AddressHandler.class);
    
    private static final String TYPE = "connect-back-request";
    
    private final EventBroadcaster<ConnectBackRequestedEvent> connectBackEventBroadcaster;

    private final FacebookFriendConnection connection;
    
    @AssistedInject
    public ConnectBackRequestHandler(@Assisted FacebookFriendConnection connection,
                EventBroadcaster<ConnectBackRequestedEvent> connectBackEventBroadcaster) {
        this.connection = connection;
        this.connectBackEventBroadcaster = connectBackEventBroadcaster;
    }

    @Inject
    @Override
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }
    
    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        assert messageType.equals(TYPE);
        GUID clientGuid;
        try {
            clientGuid = new GUID(message.getString("client-guid"));
        } catch (IllegalArgumentException iae) {
            LOG.debug("invalid guid", iae);
            throw new JSONException(iae);
        }
        int fwtVersion = message.getInt("supported-fwt-version");
        Connectable address = deserializeAddress(message.getJSONObject("address"));
        connectBackEventBroadcaster.broadcast(new ConnectBackRequestedEvent(new ConnectBackRequest(address, clientGuid, fwtVersion)));
    }
    
    static Connectable deserializeAddress(JSONObject address) throws JSONException {
        ConnectableSerializer serializer = new ConnectableSerializer();
        String type = address.getString("type"); 
        if (!type.equals(serializer.getAddressType())) {
            LOG.debugf("invalid address type: {0}", type);
            throw new JSONException("wrong address type: " + type);
        }
        try {
            return serializer.deserialize(Base64.decodeBase64(StringUtils.toUTF8Bytes(address.getString("value"))));
        } catch (IOException ie) {
            LOG.debug("invalid address", ie);
            throw new JSONException(ie);
        }
    }

    @Override
    public void sendFeature(FriendPresence presence, ConnectBackRequest data)
            throws FriendException {
        try {
            Map<String, Object> message = new HashMap<String, Object>();
            message.put("client-guid", data.getClientGuid().toHexString());
            message.put("supported-fwt-version", data.getSupportedFWTVersion());
            
            Map<String, Object> address = new HashMap<String, Object>();
            ConnectableSerializer serializer = new ConnectableSerializer();
            address.put("type", serializer.getAddressType());
            address.put("value", StringUtils.toUTF8String(Base64.encodeBase64(serializer.serialize(data.getAddress()))));
            
            message.put("address", address);
            
            connection.sendLiveMessage(presence, TYPE, message);
        } catch (IOException ie) {
            throw new RuntimeException(ie);
        }
    }

}
