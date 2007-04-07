package com.limegroup.gnutella.dht.db;

import java.util.Collection;
import java.util.Collections;

import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueEntityPublisher;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.util.CollectionUtils;
import org.limewire.mojito.util.DatabaseUtils;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.util.KUIDUtils;
import com.limegroup.gnutella.settings.DHTSettings;

/**
 * This class has currently no practical use-case.
 */
public class PushProxiesPublisher implements DHTValueEntityPublisher {
    
    private final MojitoDHT dht;
    
    private DHTValueEntity localhost = null;
    
    public PushProxiesPublisher(MojitoDHT dht) {
        this.dht = dht;
    }
    
    private synchronized DHTValueEntity getPushProxyForSelf() {
        if (RouterService.acceptedIncomingConnection()) {
            return null;
        }
        
        if (localhost == null) {
            GUID guid = new GUID(RouterService.getMyGUID());
            KUID primaryKey = KUIDUtils.toKUID(guid);
            
            DHTValueFactory valueFactory = dht.getDHTValueFactory();
            localhost = valueFactory.createDHTValueEntity(
                    dht.getLocalNode(), 
                    dht.getLocalNode(), 
                    primaryKey, 
                    PushProxiesDHTValueImpl.FOR_SELF, 
                    true);
        }
        
        return localhost;
    }
    
    public Collection<DHTValueEntity> get(KUID primaryKey) {
        if (!DHTSettings.PUBLISH_PUSH_PROXIES.getValue()) {
            return Collections.emptySet();
        }
        
        DHTValueEntity localhost = getPushProxyForSelf();
        if (localhost != null 
                && localhost.getKey().equals(primaryKey)) {
            return Collections.singleton(localhost);
        }
        return Collections.emptySet();
    }
    
    public Collection<DHTValueEntity> getValues() {
        if (!DHTSettings.PUBLISH_PUSH_PROXIES.getValue()) {
            return Collections.emptySet();
        }
        
        DHTValueEntity localhost = getPushProxyForSelf();
        if (localhost != null) {
            return Collections.singleton(localhost);
        }
        return Collections.emptySet();
    }

    public Collection<DHTValueEntity> getValuesToForward() {
        if (!DHTSettings.PUBLISH_PUSH_PROXIES.getValue()) {
            return Collections.emptySet();
        }
        
        return getValues();
    }

    public Collection<DHTValueEntity> getValuesToPublish() {
        if (!DHTSettings.PUBLISH_PUSH_PROXIES.getValue()) {
            return Collections.emptySet();
        }
        
        DHTValueEntity localhost = getPushProxyForSelf();
        if (localhost != null 
                && DatabaseUtils.isPublishingRequired(localhost)) {
            return Collections.singleton(localhost);
        }
        return Collections.emptySet();
    }
    
    public synchronized void changeContact(Contact node) {
        this.localhost = null;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder("PushProxiesPublisher: ");
        buffer.append(CollectionUtils.toString(getValues()));
        return buffer.toString();
    }
}
