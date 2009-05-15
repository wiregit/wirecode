package org.limewire.core.api.friend.feature;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.ParameterizedType;

public class FeatureTransportHandlerRegistryImpl implements FeatureTransportHandlerRegistry{
    
    private final Map<Class, FeatureTransport.Handler> featureTransports;
    
    public FeatureTransportHandlerRegistryImpl() {
        this.featureTransports = new ConcurrentHashMap<Class, FeatureTransport.Handler>();
    }
    
    @Override
    public <T> void register(Class<T> c, FeatureTransport.Handler<T> handler) {
        featureTransports.put(c, handler);
    }

    @Override
    public <T extends Feature<U>, U> FeatureTransport.Handler<U> getHandler(Class<T> feature) {
        java.lang.reflect.Type type = feature.getGenericSuperclass();
        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            java.lang.reflect.Type [] typeArgs = parameterizedType.getActualTypeArguments();
            if(typeArgs != null && typeArgs.length > 0) {
                java.lang.reflect.Type typeArg = typeArgs[0];
                if(typeArg instanceof Class) {
                    return (FeatureTransport.Handler<U>)featureTransports.get((Class) typeArg);
                }
            }
        }
        return null;
    }
}
