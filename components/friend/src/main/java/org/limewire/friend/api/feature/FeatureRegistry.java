package org.limewire.friend.api.feature;

import java.net.URI;

/**
 * A registry of all of the locally supported features.
 *
 * For each <cod>Feature</code> there is a <code>FeatureInitializer</code> that
 * is invoked on a <code>FriendPresence</code> after it is discovered
 * that that presence supports the feature.<p>
 *
 * A <code>FeatureInitializer</code> might, for example, send the
 * local address to the friend presence.
 */
public interface FeatureRegistry extends Iterable<URI>{

    /**
     * Adds a <code>FeatureInitializer</code> to the registry for
     * a specific <code>URI</code> id.
     * @param uri the id of the <code>Feature</code>
     * @param featureInitializer
     */
    void add(URI uri, FeatureInitializer featureInitializer);

    /**
     * @param uri
     * @return the <code>FeatureInitializer</code the <code>uri</code>
     * or null if it does not exist
     */
    FeatureInitializer get(URI uri);
}
