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

/**
 * Ultrapeers (passive DHT Nodes) have some special republishing
 * rules for their firewalled leafs
 * 
 * TODO: Parts of isExpired() and isRepublishingRequired() apply also
 * for regular DHT Nodes!
 */
class PassiveDHTNodeRepublishManager extends DefaultRepublishManager {
    
    /*@Override
    public boolean isExpired(Context context, DHTValueEntity entity) {
        if (super.isExpired(context, entity)) {
            return true;
        }
        
        if (entity.isLocalValue()) {
            DHTValue value = entity.getValue();
            if (value instanceof AltLocDHTValue) {
                AltLocDHTValue altLoc = (AltLocDHTValue)value;
                
                // If it's an AltLoc for the localhost then
                // check if the FileDesc still exists. If no
                // then remove it from the Database!
                if (altLoc.isLocalAltLoc()) {
                    URN urn = LimeDHTManager.toURN(entity.getKey());
                    FileManager fileManager = RouterService.getFileManager();
                    FileDesc fd = fileManager.getFileDescForUrn(urn);
                    if (fd == null) {
                        return true;
                    }
                }
                
                // If it's NOT an AltLoc for the localhost
                // and we've published it then remove it!
                return entity.hasBeenPublished() 
                        && !altLoc.isLocalAltLoc();
            }
        }
        
        return false;
    }
    
    @Override
    public boolean isRepublishingRequired(Context context, DHTValueEntity entity) {
        if (super.isRepublishingRequired(context, entity)) {
            DHTValue value = entity.getValue();
            if (value instanceof AltLocDHTValue) {
                AltLocDHTValue altLoc = (AltLocDHTValue)value;
                
                // If it's an AltLoc for the localhost then
                // check if the FileDesc is rare. If it's not
                // rare then there's no reason to publish it!
                if (altLoc.isLocalAltLoc()) {
                    URN urn = LimeDHTManager.toURN(entity.getKey());
                    FileManager fileManager = RouterService.getFileManager();
                    FileDesc fd = fileManager.getFileDescForUrn(urn);
                    
                    if (fd == null || !LimeDHTManager.isRare(fd)) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        return false;
    }*/
}
