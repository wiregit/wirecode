package org.limewire.friend.api.feature;

public interface FeatureTransportHandlerRegistry {
    <T> void register(Class<T> c, FeatureTransport.Handler<T> handler);
    <T extends Feature<U>, U> FeatureTransport.Handler<U> getHandler(Class<T> c);
}
