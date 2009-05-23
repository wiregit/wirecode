package org.limewire.facebook.service.livemessage;

public interface LiveMessageHandlerRegistry {
    void register(String messageType, LiveMessageHandler handler);
    LiveMessageHandler getHandler(String messageType);
}
