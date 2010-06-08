package org.limewire.mojito.message;

import org.limewire.mojito.routing.Contact;

/**
 * An interface for <tt>FIND_NODE</tt> lookup response {@link Message}s.
 */
public interface NodeResponse extends LookupResponse, SecurityTokenProvider {

    /**
     * Returns the found {@link Contact}s.
     */
    public Contact[] getContacts();
}
