package org.limewire.facebook.service;

public interface LiveMessageHandlerRegistry {
    void register(LiveMessageHandler handler);
    LiveMessageHandler getHandler(String messageType);
}
