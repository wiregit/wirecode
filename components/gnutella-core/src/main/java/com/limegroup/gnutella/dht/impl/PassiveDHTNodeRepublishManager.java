package com.limegroup.gnutella.dht.impl;

import org.limewire.mojito.Context;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.impl.DefaultRepublishManager;

import com.limegroup.gnutella.dht.AltLocDHTValue;

/**
 * Ultrapeers (passive DHT Nodes) have some special republishing
 * rules for their firewalled leafs
 */
class PassiveDHTNodeRepublishManager extends DefaultRepublishManager {
    
    @Override
    public boolean isExpired(Context context, DHTValueEntity entity) {
        if (super.isExpired(context, entity)) {
            return true;
        }
        
        if (entity.isLocalValue()) {
            DHTValue value = entity.getValue();
            if (value instanceof AltLocDHTValue) {
                AltLocDHTValue altLoc = (AltLocDHTValue)value;
                
                // If it's NOT an AltLoc for the localhost
                // and we've published it then remove it!
                return entity.hasBeenPublished() 
                        && !altLoc.isLocalAltLoc();
            }
        }
        
        return false;
    }
}
