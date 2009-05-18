package org.limewire.facebook.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressFactory;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;


public class LiveMessageAddressTransport implements FeatureTransport<Address>, LiveMessageHandler {
    
    private static final String TYPE = "address";
    
    private static final Log LOG = LogFactory.getLog(LiveMessageAddressTransport.class);
    
    private final FacebookFriendConnection connection;
    private final AddressFactory addressFactory;

    @AssistedInject
    LiveMessageAddressTransport(@Assisted FacebookFriendConnection connection,
            AddressFactory addressFactory) {
        this.connection = connection;
        this.addressFactory = addressFactory;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) {
        
    }

    @Override
    public void sendFeature(FriendPresence presence, Address localFeature) throws FriendException {
        Map<String, String> message = new HashMap<String, String>();
        message.put("from", connection.getUID());
        try {
            message.put("address", StringUtils.toUTF8String(Base64.encodeBase64(addressFactory.getSerializer(localFeature.getClass()).serialize(localFeature))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.debugf("sending address: {0}", message);
        connection.sendLiveMessage(presence, TYPE, message);
    }
}
