package org.limewire.net.address.gnutella;

import java.io.IOException;
import java.net.UnknownHostException;

import org.limewire.net.address.Address;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.DirectConnectionAddress;
import org.limewire.net.address.DirectConnectionAddressImpl;
import org.limewire.net.address.DirectConnectionAddressSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PushProxyAddressSerializer extends DirectConnectionAddressSerializer {
    public String getAddressType() {
        return "push-proxy-info";
    }

    public Class<? extends Address> getAddressClass() {
        return PushProxyAddress.class;
    }

    @Inject
    public void register(AddressFactory factory) {
        super.register(factory);
    }

    public Address deserialize(byte[] serializedAddress) throws IOException {
        DirectConnectionAddress address = (DirectConnectionAddress)super.deserialize(serializedAddress);
        return new PushProxyAddressImpl(address.getAddress(), address.getPort(), address.isTLSCapable());
    }

    public byte[] serialize(Address address) throws IOException {
        return super.serialize(address);
    }
    
    private class PushProxyAddressImpl extends DirectConnectionAddressImpl implements PushProxyAddress{

        public PushProxyAddressImpl(String host, int port, boolean tlsCapable) throws UnknownHostException {
            super(host, port, tlsCapable);
        }
    }
}
