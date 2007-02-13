package com.limegroup.gnutella.dht.impl;

import org.limewire.mojito.Context;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.impl.DefaultRepublishManager;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.AltLocDHTValue;
import com.limegroup.gnutella.dht.LimeDHTUtils;
import com.limegroup.gnutella.dht.PushProxiesDHTValue;

/**
 * 
 */
class LimeRepublishManager extends DefaultRepublishManager {
    
    /**
     * 
     */
    @Override
    public boolean isExpired(Context context, DHTValueEntity entity) {
        if (super.isExpired(context, entity)) {
            return true;
        }
        
        DHTValue value = entity.getValue();
        if (entity.isLocalValue() 
                && value instanceof PushProxiesDHTValue) {
            PushProxiesDHTValue proxyValue = (PushProxiesDHTValue)value;
            return !proxyValue.isPushProxiesForSelf() 
                        && entity.hasBeenPublished();
        }
        
        return false;
    }

    /**
     * 
     */
    @Override
    public boolean isRepublishingRequired(Context context, DHTValueEntity entity) {
        if (super.isRepublishingRequired(context, entity)) {
            DHTValue value = entity.getValue();
            if (value instanceof AltLocDHTValue) {
                AltLocDHTValue altLoc = (AltLocDHTValue)value;
                // If it's an AltLoc for the localhost then
                // check if the FileDesc is rare. If it's not
                // rare then there's no reason to publish it!
                if (altLoc.isAltLocForSelf()) {
                    URN urn = LimeDHTUtils.toURN(entity.getKey());
                    FileManager fileManager = RouterService.getFileManager();
                    FileDesc fd = fileManager.getFileDescForUrn(urn);
                    return (fd != null && LimeDHTUtils.isRare(fd));
                }
            } // else if ...
        }
        return false;
    }
}
