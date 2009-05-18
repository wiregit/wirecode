package org.limewire.facebook.service;

import org.json.JSONObject;
import org.json.JSONException;

public interface LiveMessageHandler {
    void register(LiveMessageHandlerRegistry registry);
    void handle(String messageType, JSONObject message) throws JSONException;
}
