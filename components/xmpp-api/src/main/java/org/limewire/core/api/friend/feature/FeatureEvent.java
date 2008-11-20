package org.limewire.core.api.friend.feature;

import org.limewire.listener.DefaultDataEvent;
import org.limewire.xmpp.api.client.Presence;

public class FeatureEvent extends DefaultDataEvent<Presence, FeatureEvent.Type, Feature> {

    public static enum Type {
        ADDED, 
        REMOVED 
    }
    
    public FeatureEvent(Presence source, Type type, Feature feature) {
        super(source, type, feature);
    }
}


