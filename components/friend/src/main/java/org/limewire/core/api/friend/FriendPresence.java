package org.limewire.core.api.friend;

import java.net.URI;
import java.util.Collection;

import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureTransport;
import org.limewire.i18n.I18nMarker;

/**
 * A presence for a friend. One friend can have multiple presences.
 */
public interface FriendPresence {
    int MIN_PRIORITY = -127;
    int MAX_PRIORITY = 127;

    /**
     * Returns the containing friend.
     */
    Friend getFriend();

    /**
     * The ID of this specific presence. For example, an XMPP Presence would be
     * in the form of <code>user@host/resource</code> whereas a Gnutella
     * Presence would be the clientGUID.
     */
    String getPresenceId();

    /**
     * @return a Collection of Features that this FriendPresence supports
     */
    Collection<Feature> getFeatures();

    /**
     * @param id
     * @return the Feature corresponding the given id
     */
    Feature getFeature(URI id);

    /**
     * @param id
     * @return whether this FriendPresence supports all of the input feature ids
     */
    boolean hasFeatures(URI... id);

    /**
     * Adds a new Feature to this FriendPrsence
     * @param feature the feature to add
     */
    void addFeature(Feature feature);

    /**
     * Removes a feature from this FriendPresence
     * @param id the feature to remove
     */
    void removeFeature(URI id);
    
    <T extends Feature<U>, U> FeatureTransport<U> getTransport(Class<T> feature);

    <U> void addTransport(Class<U> clazz, FeatureTransport<U> transport);

    /**
     * @return the presence type
     */
    Type getType();

    /**
     * @return the presence status message; can be <code>null</code>
     */
    String getStatus();

    /**
     * @return the priority of this presence in relation to other presence's of the same user
     */
    int getPriority();

    /**
     * @return the presence mode
     */
    Mode getMode();

    enum Type {
        available, unavailable, subscribe, subscribed, unsubscribe, unsubscribed, error
    }

    /**
     * The actual presence status.
     */
    enum Mode {
        // lower case enum values to allow direct mapping from to the Mode enum
        // defined in smack
        chat(I18nMarker.marktr("Free to chat"), 0),
        available(I18nMarker.marktr("Available"), 1),
        away(I18nMarker.marktr("Away"), 2),//away and extended away given the same order for now, since they are rendered in the ui the same otherwise it would be confusing
        xa(I18nMarker.marktr("Away for a while"), 2),
        dnd(I18nMarker.marktr("Do not disturb"), 3);

        private final String name;
        private final int order;

        Mode(String name, int order) {
            this.name = name;
            this.order = order;
        }

        /**
         * @return the Order that this Mode should be sorted against other modes.
         */
        public int getOrder() {
            return order;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
