package org.limewire.core.api.friend.feature;

import java.net.URI;

public interface FeatureRegistry extends Iterable<URI>{
    void add(URI uri, FeatureInitializer featureInitializer); 
    FeatureInitializer get(URI uri);
}
