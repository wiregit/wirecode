package com.limegroup.gnutella.dht;

import org.limewire.mojito.Context;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.impl.DefaultPublishConstraint;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;

/**
 * 
 */
public class LimePublishConstraint extends DefaultPublishConstraint {
    
    /**
     * 
     */
    @Override
    public boolean isExpired(Context context, DHTValueEntity entity) {
        if (super.isExpired(context, entity)) {
            return true;
        }
        
        if (entity.isLocalValue()){
            DHTValue value = entity.getValue();
            if (value instanceof AltLocDHTValue) {
                AltLocDHTValue altLocValue = (AltLocDHTValue)value;
                return !altLocValue.isAltLocForSelf()
                            && entity.hasBeenPublished();
                
            } else if (value instanceof PushProxiesDHTValue) {
                PushProxiesDHTValue proxyValue = (PushProxiesDHTValue)value;
                return !proxyValue.isPushProxiesForSelf() 
                            && entity.hasBeenPublished();
            }
        }
        
        return false;
    }

    /**
     * 
     */
    @Override
    public boolean isPublishingRequired(Context context, DHTValueEntity entity) {
        if (super.isPublishingRequired(context, entity)) {
            DHTValue value = entity.getValue();
            if (value instanceof AltLocDHTValue) {
                AltLocDHTValue altLocValue = (AltLocDHTValue)value;
                // If it's an AltLoc for the localhost then
                // check if the FileDesc is rare. If it's not
                // rare then there's no reason to publish it!
                if (altLocValue.isAltLocForSelf()) {
                    URN urn = LimeDHTUtils.toURN(entity.getKey());
                    FileManager fileManager = RouterService.getFileManager();
                    FileDesc fd = fileManager.getFileDescForUrn(urn);
                    return (fd != null && isRare(fd));
                }
            } // else if ...
        }
        return false;
    }
    
    /**
     * Returns true if the FileDesc is considered rare
     * 
     * TODO: Define rare
     */
    private static boolean isRare(FileDesc fd) {
        long time = fd.getLastAttemptedUploadTime();
        return (System.currentTimeMillis() - time >= 0L);
    }
}
