package org.limewire.xmpp.client.impl;

import java.util.concurrent.ConcurrentHashMap;

import org.limewire.io.Address;

import com.google.inject.Singleton;

@Singleton
public class XMPPAddressRegistry {
    
    private final ConcurrentHashMap<XMPPAddress, Address> addressMap;
    
    public XMPPAddressRegistry() {
        this.addressMap = new ConcurrentHashMap<XMPPAddress, Address>();
    }
    
    public void put(XMPPAddress xmppAddress, Address address){
        addressMap.put(xmppAddress, address);
    }
    
    public Address get(XMPPAddress xmppAddress) {
       return addressMap.get(xmppAddress); 
    }
    
    public void remove(XMPPAddress xmppAddress) {
        addressMap.remove(xmppAddress);
    }
    
}
