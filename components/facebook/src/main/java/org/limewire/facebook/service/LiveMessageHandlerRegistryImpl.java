package org.limewire.facebook.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LiveMessageHandlerRegistryImpl implements LiveMessageHandlerRegistry {
    
    private final Map<String, LiveMessageHandler> handlers;
    
    @Inject
    LiveMessageHandlerRegistryImpl() {
        this.handlers = new ConcurrentHashMap<String, LiveMessageHandler>();
    }

    @Override
    public void register(LiveMessageHandler handler) {
        handlers.put(handler.getMessageType(), handler);
    }

    @Override
    public LiveMessageHandler getHandler(String messageType) {
        return handlers.get(messageType);
    }
}
