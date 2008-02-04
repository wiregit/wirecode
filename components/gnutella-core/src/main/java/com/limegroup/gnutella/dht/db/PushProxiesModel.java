package com.limegroup.gnutella.dht.db;

import java.util.Collection;
import java.util.Collections;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.Storable;
import org.limewire.mojito.db.StorableModel;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.util.DatabaseUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * The PushProxiesPublisher publishes Push Proxy information for 
 * the localhost in the DHT
 */
@Singleton
public class PushProxiesModel implements StorableModel {
    
    private Storable localhost = null;
    
    private final PushProxiesValueFactory pushProxiesValueFactory;
    
    @Inject
    public PushProxiesModel(PushProxiesValueFactory pushProxiesValueFactory) {
        this.pushProxiesValueFactory = pushProxiesValueFactory;
    }
    
    private synchronized Storable getPushProxyForSelf() {
        if (localhost == null) {
            PushProxiesValue pushProxiesValueForSelf = pushProxiesValueFactory.createDHTValueForSelf();
            if (pushProxiesValueForSelf.getPushProxies().isEmpty()) {
                return null;
            }
            
            GUID guid = new GUID(pushProxiesValueForSelf.getGUID());
            KUID primaryKey = KUIDUtils.toKUID(guid);
            localhost = new Storable(primaryKey, pushProxiesValueForSelf);
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
