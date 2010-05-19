package com.limegroup.gnutella.dht.db;

import java.util.Set;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.io.IpPort;
import org.limewire.mojito.exceptions.DHTValueException;
import org.limewire.mojito2.routing.Version;
import org.limewire.mojito2.storage.DHTValueType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointFactory;

/**
 * Factory to create {@link PushProxiesValue}s.
 */
@Singleton
public class PushProxiesValueFactoryImpl implements PushProxiesValueFactory {

    private final Provider<PushProxiesValue> lazySelf;

    @Inject
    public PushProxiesValueFactoryImpl(final NetworkManager networkManager,
            final PushEndpointFactory pushEndpointFactory, 
            final ApplicationServices applicationServices) {
        
        lazySelf = new AbstractLazySingletonProvider<PushProxiesValue>() {
            @Override
            protected PushProxiesValue createObject() {
                return new PushProxiesValueForSelf(
                        networkManager,
                        pushEndpointFactory,
                        applicationServices);
            }
        };
    }

    @Override
    public PushProxiesValue createDHTValue(DHTValueType type, Version version,
            byte[] value) throws DHTValueException {

        return createFromData(version, value);
    }

    @Override
    public PushProxiesValue createDHTValueForSelf() {
        return lazySelf.get();
    }
    
    /**
     * Factory method for testing purposes
     */
    AbstractPushProxiesValue createPushProxiesValue(Version version,
            byte[] guid, byte features, int fwtVersion, int port,
            Set<? extends IpPort> proxies) {
        return new PushProxiesValueImpl(version, guid, features, fwtVersion,
                port, proxies);
    }

    /**
     * Factory method to create PushProxiesValues
     */
    PushProxiesValue createFromData(Version version, byte[] data)
            throws DHTValueException {
        return new PushProxiesValueImpl(version, data);
    }
}
