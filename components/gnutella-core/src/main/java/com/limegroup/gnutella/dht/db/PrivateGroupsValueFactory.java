package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;


public interface PrivateGroupsValueFactory extends DHTValueFactory<PrivateGroupsValue>{
    
    public PrivateGroupsValue createDHTValue(DHTValueType type, Version version,
            byte[] value) throws DHTValueException;

    public PrivateGroupsValue createDHTValueForSelf();

}
