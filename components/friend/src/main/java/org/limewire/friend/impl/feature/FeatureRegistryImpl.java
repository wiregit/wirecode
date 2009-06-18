package org.limewire.friend.impl.feature;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FeatureRegistryImpl implements FeatureRegistry {
    
    private final Map<URI, FeatureInitializer> featureInitializerMap;
    
    @Inject
    FeatureRegistryImpl() {
        featureInitializerMap = new ConcurrentHashMap<URI, FeatureInitializer>();
    }
    
    @Override
    public void add(URI uri, FeatureInitializer featureInitializer) {
        featureInitializerMap.put(uri, featureInitializer);
    }

    @Override
    public FeatureInitializer get(URI uri) {
        return featureInitializerMap.get(uri);
    }

    @Override
    public Iterator<URI> iterator() {
        return featureInitializerMap.keySet().iterator();
    }
}
