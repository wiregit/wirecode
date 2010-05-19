package com.limegroup.gnutella.dht.db;

import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValueType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;


/**
 * Factory to create {@link AltLocValue}s.
 */
@Singleton
class AltLocValueFactoryImpl implements AltLocValueFactory {
    
    private final NetworkManager networkManager;
    private final ApplicationServices applicationServices;
    
    @Inject
    public AltLocValueFactoryImpl(NetworkManager networkManager,
            ApplicationServices applicationServices) {
        this.networkManager = networkManager;
        this.applicationServices = applicationServices;
    }
    
    @Override
    public AltLocValue createDHTValue(DHTValueType type, 
            Version version, byte[] value) throws DHTValueException {
        
        return createFromData(version, value);
    }

    @Override
    public AltLocValue createAltLocValueForSelf(long fileSize, byte[] ttroot) {
        return new AltLocValueForSelf(fileSize, ttroot, networkManager, applicationServices);
    }
    
    @Override
    public AltLocValue createFromData(Version version, byte[] data) throws DHTValueException {
        return new AltLocValueImpl(version, data);
    }
    
    /**
     * Factory method for testing purposes.
     */
    AltLocValue createAltLocValue(Version version, byte[] guid, int port, 
            long length, byte[] ttroot, boolean firewalled) {
        return new AltLocValueImpl(version, guid, port, length, ttroot, firewalled, false);
    }
    
}

