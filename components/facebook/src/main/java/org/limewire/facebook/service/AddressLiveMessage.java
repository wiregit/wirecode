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
import org.limewire.net.address.AddressFactory;
import org.limewire.util.StringUtils;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;


public class AddressLiveMessage implements FeatureTransport<Address> {
    
    private final Provider<String> apiKey;
    private final FacebookFriendConnection connection;
    private final AddressFactory addressFactory;

    @Inject
    AddressLiveMessage(@Named("facebookApiKey") Provider<String> apiKey,
                       FacebookFriendConnection connection,
                       AddressFactory addressFactory) {
        this.apiKey = apiKey;
        this.connection = connection;
        this.addressFactory = addressFactory;
    }
   
    
    @Override
    public void sendFeature(FriendPresence presence, Address localFeature) throws FriendException {
        FacebookJsonRestClient client = new FacebookJsonRestClient(apiKey.get(), connection.getSecret(), connection.getSession());
        try {
            Map<String, String> message = new HashMap<String, String>();
            message.put("from", connection.getUID());
            message.put("address", StringUtils.toUTF8String(Base64.encodeBase64(addressFactory.getSerializer(localFeature.getClass()).serialize(localFeature))));
            client.liveMessage_send(Long.parseLong(presence.getFriend().getId()), "address",
                    new JSONObject(message));
        } catch (FacebookException e) {
            throw new FriendException(e);
        } catch (IOException e) {
            throw new FriendException(e);
        }
    }
}
