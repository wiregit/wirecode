package org.limewire.mojito.message;

import org.limewire.mojito.KUID;

/**
 * An interface for lookup {@link RequestMessage}s.
 */
public interface LookupRequest extends RequestMessage {

    /**
     * Returns the lookup {@link KUID}.
     */
    public KUID getLookupId();
}
