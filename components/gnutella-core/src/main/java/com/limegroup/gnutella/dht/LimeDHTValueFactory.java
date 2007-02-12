package com.limegroup.gnutella.dht;

import org.limewire.mojito.KUID;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.impl.DefaultDHTValueFactory;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;

public class LimeDHTValueFactory implements DHTValueFactory {
    
    public DHTValue createDHTValue(DHTValueType valueType, Version version, byte[] value) 
            throws DHTValueException {
        
        if (valueType.equals(AltLocDHTValueImpl.ALT_LOC)) {
            return AltLocDHTValueImpl.createFromData(valueType, version, value);
            
        } else if (valueType.equals(PushProxiesDHTValueImpl.PUSH_PROXIES)) {
            return PushProxiesDHTValueImpl.createFromData(valueType, version, value);
        }
        
        return DefaultDHTValueFactory.FACTORY.createDHTValue(valueType, version, value);
    }

    public DHTValueEntity createDHTValueEntity(Contact creator, Contact sender, 
            KUID primaryKey, DHTValue value, boolean localValue) {
        
        return DefaultDHTValueFactory.FACTORY.createDHTValueEntity(creator, sender, 
                primaryKey, value, localValue);
    }
}
