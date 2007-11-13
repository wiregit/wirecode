package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito.routing.Version;

import com.google.inject.Inject;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;

public class PrivateGroupsValueFactoryImpl implements PrivateGroupsValueFactory{

    private NetworkManager networkManager;
    private ApplicationServices applicationServices;

    @Inject
    public PrivateGroupsValueFactoryImpl (NetworkManager networkManager,
            ApplicationServices applicationServices) {
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
    }
    
    public PrivateGroupsValue createDHTValue(DHTValueType type, Version version, byte[] value)
            throws DHTValueException {
        return createFromData(version, value);
    }



    public PrivateGroupsValue createDHTValueForSelf() {
        
        return new PrivateGroupsValueForSelf(networkManager, applicationServices);
    }
    

    /**
     * Factory method to create PrivateGroupsValue 
     */
    public PrivateGroupsValue createFromData(Version version, byte[] data) throws DHTValueException {
        
        return new PrivateGroupsValueImpl(version, data);
    }

    /**
     * Factory method for testing purposes
     */
    PrivateGroupsValue createPrivateGroupsValue(Version version, byte[] guid, int port, 
            long length) {
        return new PrivateGroupsValueImpl(version, guid, port, length);
    }
    
}
