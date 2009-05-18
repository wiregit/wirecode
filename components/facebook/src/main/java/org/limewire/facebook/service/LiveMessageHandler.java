package org.limewire.facebook.service;

import org.json.JSONObject;
import org.json.JSONException;

public interface LiveMessageHandler {
    void register(LiveMessageHandlerRegistry registry);
    void handle(JSONObject message) throws JSONException;
}
