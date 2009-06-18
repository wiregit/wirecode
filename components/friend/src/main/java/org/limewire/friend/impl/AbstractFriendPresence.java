package org.limewire.friend.impl;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListenerList;

public abstract class AbstractFriendPresence implements FriendPresence {
    
    private final Map<URI, Feature> features;
    private final Map<Class, FeatureTransport> featureTransports;
    private final EventBroadcaster<FeatureEvent> featureBroadcaster;
    
    public AbstractFriendPresence() {
        this(new EventListenerList<FeatureEvent>());
    }

    public AbstractFriendPresence(EventBroadcaster<FeatureEvent> featureEventBroadcaster){
        this.features = new ConcurrentHashMap<URI, Feature>();
        this.featureTransports = new ConcurrentHashMap<Class, FeatureTransport>();
        this.featureBroadcaster = featureEventBroadcaster;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return features.values();
    }

    @Override
    public Feature getFeature(URI id) {
        return features.get(id);
    }

    @Override
    public boolean hasFeatures(URI... id) {
        for(URI uri : id) {
            if(getFeature(uri) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
        featureBroadcaster.broadcast(new FeatureEvent(this, FeatureEvent.Type.ADDED, feature));
    }

    @Override
    public void removeFeature(URI id) {
        Feature feature = features.remove(id);
        if(feature != null) {
            featureBroadcaster.broadcast(new FeatureEvent(this, FeatureEvent.Type.REMOVED, feature));
        }
    }

    @Override
    public <T extends Feature<U>, U> FeatureTransport<U> getTransport(Class<T> feature) {
        java.lang.reflect.Type type = feature.getGenericSuperclass();
        if(type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType)type;
            java.lang.reflect.Type [] typeArgs = parameterizedType.getActualTypeArguments();
            if(typeArgs != null && typeArgs.length > 0) {
                java.lang.reflect.Type typeArg = typeArgs[0];
                if(typeArg instanceof Class) {
                    return featureTransports.get(typeArg);
                }
            }
        }
        return null;
    }

    @Override
    public <U> void addTransport(Class<U> clazz, FeatureTransport<U> transport) {
        featureTransports.put(clazz, transport);
    }
}
