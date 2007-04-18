package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

/**
 * Factory to create PushProxiesValues
 */
public class PushProxiesValueFactory implements DHTValueFactory<PushProxiesValue> {

    public PushProxiesValue createDHTValue(DHTValueType type, 
            Version version, byte[] value) throws DHTValueException {
        
        return PushProxiesValue.createFromData(version, value);
    }
}
