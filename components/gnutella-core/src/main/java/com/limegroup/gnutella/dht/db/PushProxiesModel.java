package com.limegroup.gnutella.dht.db;

import java.util.Collection;
import java.util.Collections;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.util.DatabaseUtils;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * The PushProxiesPublisher publishes Push Proxy information for 
 * the localhost in the DHT
 */
public class PushProxiesModel implements StorableModel {
    
    private Storable localhost = null;
    
    private synchronized Storable getPushProxyForSelf() {
        if (RouterService.acceptedIncomingConnection()) {
            return null;
        }
        
        if (localhost == null) {
            GUID guid = new GUID(RouterService.getMyGUID());
            KUID primaryKey = KUIDUtils.toKUID(guid);
            
            localhost = new Storable(
                    primaryKey, PushProxiesValue.FOR_SELF);
        }
        
        return localhost;
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValuePublisherModel#getPublishableEntities()
     */
    public Collection<Storable> getStorables() {
        if (!DHTSettings.PUBLISH_PUSH_PROXIES.getValue()) {
            return Collections.emptySet();
        }
        
        Storable localhost = getPushProxyForSelf();
        if (localhost != null 
                && DatabaseUtils.isPublishingRequired(localhost)) {
            return Collections.singleton(localhost);
        }
        return Collections.emptySet();
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.DHTValuePublisherModel#published(org.limewire.mojito.db.PublishableEntity)
     */
    public void handleStoreResult(Storable storable, StoreResult result) {
    }
    
    /*
     * (non-Javadoc)
     * @see org.limewire.mojito.db.StorableModel#handleContactChange()
     */
    public synchronized void handleContactChange() {
        this.localhost = null;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder("PushProxiesPublisher: ");
        buffer.append(getPushProxyForSelf());
        return buffer.toString();
    }
}
