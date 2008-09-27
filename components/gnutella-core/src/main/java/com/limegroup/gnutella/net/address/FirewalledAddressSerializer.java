package com.limegroup.gnutella.net.address;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import org.limewire.io.Address;
import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.Connectable;
import org.limewire.io.GGEP;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.net.address.ConnectableSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.GGEPKeys;

@Singleton
public class FirewalledAddressSerializer implements AddressSerializer {

    static final String PUBLIC_ADDRESS = "PU";
    static final String PRIVATEADDRESS = "PR";
    static final String PROXIES = "PX";
    static final String FWT_VERSION = GGEPKeys.GGEP_HEADER_FW_TRANS;
    static final String GUID = "GU";
    
    private final ConnectableSerializer serializer;

    @Inject
    public FirewalledAddressSerializer(ConnectableSerializer serializer) {
        this.serializer = serializer;
    }
    
    @Inject
    @Override
    public void register(AddressFactory factory) {
        factory.addSerializer(this);
    }

    @Override
    public Class<? extends Address> getAddressClass() {
        return FirewalledAddress.class;
    }

    @Override
    public String getAddressType() {
        return "firewalled-address";
    }
    
    @Override
    public FirewalledAddress deserialize(byte[] serializedAddress) throws IOException {
        try {
            GGEP ggep = new GGEP(serializedAddress, 0);
            Connectable publicAddress = serializer.deserialize(ggep.getBytes(PUBLIC_ADDRESS));
            Connectable privateAddress = serializer.deserialize(ggep.getBytes(PRIVATEADDRESS));
            GUID clientGuid = new GUID(ggep.getBytes(GUID));
            Set<Connectable> pushProxies = serializer.deserializeSet(new ByteArrayInputStream(ggep.getBytes(PROXIES)));
            int fwtVersion = ggep.getInt(FWT_VERSION);
            return new FirewalledAddress(publicAddress, privateAddress, clientGuid, pushProxies, fwtVersion);
        } catch (BadGGEPBlockException e) {
            throw new IOException(e);
        } catch (BadGGEPPropertyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] serialize(Address addr) throws IOException {
        FirewalledAddress address = (FirewalledAddress)addr;
        GGEP ggep = new GGEP();
        ggep.put(PUBLIC_ADDRESS, serializer.serialize(address.getPublicAddress()));
        ggep.put(PRIVATEADDRESS, serializer.serialize(address.getPrivateAddress()));
        ggep.put(PROXIES, serializer.serialize(address.getPushProxies()));
        ggep.put(FWT_VERSION, address.getFwtVersion());
        ggep.put(GUID, address.getClientGuid().bytes());
        return ggep.toByteArray();
    }

}
