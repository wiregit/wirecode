package com.limegroup.gnutella.dht;

import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DefaultDHTValueFactory;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

public class LimeDHTValueFactory implements DHTValueFactory {
    
    public DHTValue createDHTValue(DHTValueType type, Version version, byte[] value) throws DHTValueException {
        if (type.equals(AltLocDHTValueImpl.ALT_LOC)) {
            return AltLocDHTValueImpl.createFromData(type, version, value);
        } else if (type.equals(PushProxiesDHTValue.PUSH_PROXIES)) {
            return new PushProxiesDHTValue(type, version, value);
        }
        
        return DefaultDHTValueFactory.FACTORY.createDHTValue(type, version, value);
    }
}
