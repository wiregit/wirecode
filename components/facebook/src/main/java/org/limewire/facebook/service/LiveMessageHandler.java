package org.limewire.facebook.service;

import org.json.JSONObject;

public interface LiveMessageHandler {
    void register(LiveMessageHandlerRegistry registry);
    String getMessageType();
    void handle(JSONObject message);
}
