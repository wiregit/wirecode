package org.limewire.net.address;

import com.google.inject.Singleton;

import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class AddressFactoryImpl implements AddressFactory {
    private static final Log LOG = LogFactory.getLog(AddressFactoryImpl.class);

    private ConcurrentHashMap<String, AddressSerializer> serializerTypeMap = new ConcurrentHashMap<String, AddressSerializer>();
    private ConcurrentHashMap<Class<? extends Address>, AddressSerializer> serializerClassMap = new ConcurrentHashMap<Class<? extends Address>, AddressSerializer>();

    public void registerSerializer(AddressSerializer serializer) {
        LOG.debugf("adding serializer type = {0}, class = {1}", serializer.getAddressType(), serializer.getAddressClass());
        serializerTypeMap.put(serializer.getAddressType(), serializer);
        serializerClassMap.put(serializer.getAddressClass(), serializer);
    }

    public AddressSerializer getSerializer(Class<? extends Address> addressClass) {
        Class [] interfaces = addressClass.getInterfaces();
        AddressSerializer serializer = serializerClassMap.get(addressClass);
        if(serializer != null) {
            return serializer;
        } else {
            for(Class interfase : interfaces) {
                serializer = serializerClassMap.get(interfase);
                if(serializer != null) {
                    // TODO subclasses
                    return serializer;
                }
            }
        }
        throw new IllegalArgumentException("no serializer available for: " + addressClass);
    }

    public AddressSerializer getSerializer(String addressType) {
        return serializerTypeMap.get(addressType);
    }

    public Address deserialize(String type, byte[] serializedAddress) throws IOException {
        AddressSerializer serializer = serializerTypeMap.get(type);
        if(serializer != null) {
            return serializer.deserialize(serializedAddress);
        }
        throw new IOException("unknown message type: " + type);
    }

}
