package org.limewire.xmpp.client.impl;

import org.limewire.io.PermanentAddress;
import org.limewire.util.Objects;

/**
 * Provides a permanent address for a full jabber id including
 * its resource. 
 */
public class XMPPAddress implements PermanentAddress {

    private final String id;

    /**
     * 
     * @param id the full jabber id including resource
     */
    public XMPPAddress(String id) {
        this.id = Objects.nonNull(id, "id");
    }
    
    /**
     * Returns the full jabber id including resource. 
     */
    public String getId() {
        return id;
    }
            
}
