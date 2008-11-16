package org.limewire.xmpp.client.impl;

import org.jivesoftware.smack.util.StringUtils;
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
    public String getFullId() {
        return id;
    }
    
    /**
     * Returns the jabber id email address without resource. 
     */
    public String getId() {
        return StringUtils.parseBareAddress(id);
    }

    @Override
    public String toString() {
        return org.limewire.util.StringUtils.toString(this);
    }
}
