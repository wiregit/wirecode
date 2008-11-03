package org.limewire.core.api.friend.feature;

import org.limewire.listener.DefaultEvent;

public class FeatureEvent extends DefaultEvent<Feature, Feature.EventType> {

    public FeatureEvent(Feature source, Feature.EventType event) {
        super(source, event);
    }
}
