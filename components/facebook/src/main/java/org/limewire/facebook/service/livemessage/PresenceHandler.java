package org.limewire.facebook.service.livemessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class PresenceHandler implements LiveMessageHandler{

    private static final String TYPE = "presence";

    private static final Log LOG = LogFactory.getLog(PresenceHandler.class);

    private final FacebookFriendConnection connection;

    @AssistedInject
    public PresenceHandler(@Assisted FacebookFriendConnection connection) {
        this.connection = connection;
    }

    //private final FeatureTransport.Handler<Address> addressHandler;

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        String from = message.optString("from", null);
        String addressType = message.optString("guid", null);
        String addressData = message.optString("session-id", null);

    }
}
