package org.limewire.friend.api;

import java.net.URI;

import org.limewire.friend.api.feature.Feature;

/**
 * A presence for a friend. One friend can have multiple presences.
 */
public interface FriendPresence {

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
     * @param id
     * @return the Feature corresponding the given id
     */
    Feature getFeature(URI id);

    /**
     * @param id
     * @return whether this FriendPresence supports all of the input feature ids
     */
    boolean hasFeatures(URI... id);
}
