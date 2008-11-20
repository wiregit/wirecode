package org.limewire.xmpp.client.impl;

import java.net.URI;
import java.util.Map;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.io.Address;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;

class PresenceImpl implements Presence {

    @SuppressWarnings("unused")
    private static final Log LOG = LogFactory.getLog(PresenceImpl.class);

    private final org.jivesoftware.smack.packet.Presence presence;
    private final User user;
    private Map<URI, Feature> features;
    private EventListenerList<FeatureEvent> featureListeners;

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence,
                 User user) {
        this.presence = presence;
        this.user = user;
        features = new ConcurrentHashMap<URI, Feature>();
        featureListeners = new EventListenerList<FeatureEvent>();
    }

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence,
                 PresenceImpl currentPresence) {
        this(presence, currentPresence.getUser());
        this.features = currentPresence.features;
        this.featureListeners = currentPresence.featureListeners;
    }

    @Override
    public String getJID() {
        return presence.getFrom();
    }

    @Override
    public Type getType() {
        return Type.valueOf(presence.getType().toString());
    }

    @Override
    public String getStatus() {
        return presence.getStatus();
    }

    @Override
    public int getPriority() {
        return presence.getPriority();
    }

    @Override
    public Mode getMode() {
        return presence.getMode() != null ? Mode.valueOf(presence.getMode().toString()) : Mode.available;
    }

    public String toString() {
        return getJID() + " for " + user.toString();
    }

    @Override
    public User getUser() {
        return user;
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
    public Friend getFriend() {
        return user;
    }

    @Override
    public String getPresenceId() {
        return getJID();
    }

    public Address getPresenceAddress() {
        return null;
    }

    @Override
    public ListenerSupport<FeatureEvent> getFeatureListenerSupport() {
        return featureListeners;
    }

    @Override
    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
        featureListeners.broadcast(new FeatureEvent(this, FeatureEvent.Type.ADDED, feature));
    }

    @Override
    public void removeFeature(URI id) {
        Feature feature = features.remove(id);
        if(feature != null) {
            featureListeners.broadcast(new FeatureEvent(this, FeatureEvent.Type.REMOVED, feature));
        }
    }
}
