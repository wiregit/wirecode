package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

/**
 * Factory to create AltLocValues
 */
public class AltLocValueFactory implements DHTValueFactory<AltLocValue> {

    public AltLocValue createDHTValue(DHTValueType type, 
            Version version, byte[] value) throws DHTValueException {
        
        return AltLocValue.createFromData(version, value);
    }
}
