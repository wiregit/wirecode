package org.limewire.net.address;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Singleton;

@Singleton
public class AddressFactoryImpl implements AddressFactory {
    private ConcurrentHashMap<String, AddressSerializer> serializerTypeMap = new ConcurrentHashMap<String, AddressSerializer>();
    private ConcurrentHashMap<Class<? extends Address>, AddressSerializer> serializerClassMap = new ConcurrentHashMap<Class<? extends Address>, AddressSerializer>();

    public void addSerializer(AddressSerializer serializer) {
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
        return null;
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

    public byte[] serialize(Address address) throws IOException {
        AddressSerializer serializer = getSerializer(address.getClass());
        if(serializer != null) {
            return serializer.serialize(address);
        }
        throw new IOException("unknown message type: " + address.getClass());
    }
}
